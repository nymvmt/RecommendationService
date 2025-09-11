package com.example.recommendation.service;

import com.example.recommendation.dto.AssignItemsRequest;
import com.example.recommendation.dto.AssignItemsResponse;
import com.example.recommendation.dto.AssignmentCard;
import com.example.recommendation.dto.Participant;
import com.example.recommendation.dto.ReceiptItem;
import com.example.recommendation.dto.ReceiptOcrRequest;
import com.example.recommendation.dto.ReceiptOcrResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

@Service
public class ReceiptOcrService {

    private final WebClient client;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${azure.docintel.apiVersion}")
    private String apiVersion;

    @Value("${azure.docintel.modelId}")
    private String modelId;

    public ReceiptOcrService(WebClient docIntelClient) {
        this.client = docIntelClient;
    }

    public ReceiptOcrResponse analyzeByUrl(ReceiptOcrRequest req) {
        String analyzeUrl = String.format(
                "/documentintelligence/documentModels/%s:analyze?_overload=analyzeDocument&api-version=%s",
                modelId, apiVersion);

        String bodyJson = "{\"urlSource\":\"" + escape(req.imageUrl()) + "\"}";

        ResponseEntity<Void> start = client.post()
                .uri(analyzeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyJson)
                .retrieve()
                .toBodilessEntity()
                .block();

        String opLoc = start.getHeaders().getFirst("Operation-Location");
        if (opLoc == null) throw new IllegalStateException("Missing Operation-Location");

        JsonNode result = pollResult(opLoc);
        return parseResult(result);
    }

    public ReceiptOcrResponse analyzeByFile(MultipartFile file) {
        try {
            String analyzeUrl = String.format(
                    "/documentintelligence/documentModels/%s:analyze?_overload=analyzeDocument&api-version=%s",
                    modelId, apiVersion);

            MediaType type = MediaType.APPLICATION_OCTET_STREAM;
            if (file.getContentType() != null && !file.getContentType().isBlank()) {
                try { type = MediaType.parseMediaType(file.getContentType()); } catch (Exception ignored) {}
            }

            ResponseEntity<Void> start = client.post()
                    .uri(analyzeUrl)
                    .contentType(type)
                    .bodyValue(file.getBytes()) // 바이너리 그대로 전송
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            String opLoc = start.getHeaders().getFirst("Operation-Location");
            if (opLoc == null) throw new IllegalStateException("Missing Operation-Location");

            JsonNode result = pollResult(opLoc);
            return parseResult(result);
        } catch (Exception e) {
            throw new RuntimeException("file analyze failed: " + e.getMessage(), e);
        }
    }

    private JsonNode pollResult(String opLoc) {
        JsonNode result = null;
        for (int i = 0; i < 20; i++) {
            result = client.get().uri(opLoc)
                    .retrieve().bodyToMono(JsonNode.class).block();
            String status = result.path("status").asText();
            if ("succeeded".equalsIgnoreCase(status)) break;
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        if (result == null || !"succeeded".equalsIgnoreCase(result.path("status").asText())) {
            throw new IllegalStateException("Analysis not completed");
        }
        return result;
    }

    private ReceiptOcrResponse parseResult(JsonNode result) {
        JsonNode fields = result.path("analyzeResult").path("documents").path(0).path("fields");
        String merchant = fields.path("MerchantName").path("content").asText("");

        // 안전 파서 사용
        BigDecimal total = num(fields.path("Total"));

        List<ReceiptItem> items = new ArrayList<>();

        JsonNode itemsField = fields.path("Items");
        if (itemsField != null && !itemsField.isMissingNode()) {
            // v4 스타일 후보: "values"
            JsonNode arr = itemsField.path("values");
            // v3.1 스타일 후보: "valueArray"
            if (!arr.isArray()) arr = itemsField.path("valueArray");

            if (arr != null && arr.isArray()) {
                for (JsonNode itm : arr) {
                    // v4 스타일 후보: "properties"
                    JsonNode p = itm.path("properties");
                    // v3.1 스타일 후보: "valueObject"
                    if (p == null || p.isMissingNode() || p.isNull()) p = itm.path("valueObject");
                    if (p == null || p.isMissingNode() || p.isNull()) continue;

                    String desc = p.path("Description").path("content").asText(
                            p.path("Name").path("content").asText(""));

                    BigDecimal qty = num(p.path("Quantity"));
                    if (qty.compareTo(BigDecimal.ZERO) == 0) qty = BigDecimal.ONE;

                    BigDecimal unit = num(p.path("Price"));
                    if (unit.compareTo(BigDecimal.ZERO) == 0) unit = num(p.path("UnitPrice"));

                    BigDecimal line = num(p.path("TotalPrice"));

                    items.add(new ReceiptItem(desc, qty, unit, line));
                }
            }
        }

        return new ReceiptOcrResponse(merchant, total, items);
    }

    private String escape(String s) {
        if (s == null) return "";
        return new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
                .replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // 통합 숫자 파서: valueCurrency.amount / valueNumber / content 순서로 시도
    private BigDecimal num(JsonNode field) {
        if (field == null || field.isMissingNode()) return BigDecimal.ZERO;

        // 1) 통화형이면 amount 우선
        JsonNode amount = field.path("valueCurrency").path("amount");
        if (amount.isNumber()) return amount.decimalValue();

        // 2) 일반 숫자형
        JsonNode vn = field.path("valueNumber");
        if (vn.isNumber()) return vn.decimalValue();

        // 3) 문자열(content) 정규화
        String s = field.path("content").asText("");
        return safeBigDecimal(s, "0");
    }

    // 문자열을 BigDecimal로 안전 변환 (천단위 , / 소수점 , . 처리)
    private BigDecimal safeBigDecimal(String raw, String defVal) {
        if (raw == null) raw = defVal;
        String s = raw.trim();
        if (s.isEmpty()) s = defVal;

        // 숫자/부호/구분자만 남기기
        s = s.replaceAll("[^0-9,\\.\\-]", "");

        int lastDot = s.lastIndexOf('.');
        int lastComma = s.lastIndexOf(',');

        if (lastComma >= 0 && lastDot < 0) {
            // 콤마만 있다 = 소수점이 , 라고 가정
            s = s.replace(',', '.');
        } else if (lastComma >= 0 && lastDot >= 0) {
            // 둘 다 있다 = 오른쪽 기호를 소수점으로 가정
            if (lastComma > lastDot) {
                s = s.replace(".", "");     // 점은 천단위로 보고 제거
                s = s.replace(',', '.');    // 콤마를 소수점으로
            } else {
                s = s.replace(",", "");     // 콤마는 천단위로 보고 제거
            }
        } // 점만 있거나 아무것도 없으면 그대로

        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            // 최종 백업: 콤마 제거 후 재시도
            try { return new BigDecimal(s.replace(",", "")); }
            catch (Exception ex) { return new BigDecimal(defVal); }
        }
    }

    // 총액 랜덤 분배 (합계 보존, 마지막 사람에서 오차 보정)
    public List<BigDecimal> splitAmount(BigDecimal total, int people, Integer roundUnit, Long seed) {
        if (total == null || total.signum() <= 0) throw new IllegalArgumentException("total must be > 0");
        if (people <= 0) throw new IllegalArgumentException("people must be >= 1");
        int unit = (roundUnit == null || roundUnit <= 0) ? 1 : roundUnit;

        long T = total.setScale(0, java.math.RoundingMode.HALF_UP).longValue(); // KRW 가정: 정수
        if (people == 1) return List.of(BigDecimal.valueOf(T));

        // 랜덤 가중치 생성
        Random rnd = (seed == null) ? new SecureRandom() : new Random(seed);
        long[] r = new long[people];
        long sumR = 0;
        for (int i = 0; i < people; i++) {
            r[i] = 1 + rnd.nextInt(1000); // 1~1000
            sumR += r[i];
        }

        // 1차 배분(내림) + 반올림 단위 적용
        long[] alloc = new long[people];
        long used = 0;
        for (int i = 0; i < people; i++) {
            double share = (double) T * r[i] / sumR;
            long amt = Math.round(share); // 먼저 정수 가깝게
            if (unit > 1) {
                // 단위 반올림: 가장 가까운 unit 배수로
                long q = Math.round((double) amt / unit);
                amt = q * unit;
            }
            alloc[i] = Math.max(0, amt);
            used += alloc[i];
        }

        // 합계 보정: 마지막 사람에게 델타 반영
        long delta = T - used;
        alloc[people - 1] += delta; // delta가 음수/양수 모두 허용 → 합계 == T 보장

        // 음수 방지(극단적인 경우) 및 마지막에 다시 합 맞춤
        long sum2 = 0;
        for (int i = 0; i < people; i++) {
            if (alloc[i] < 0) alloc[i] = 0;
            sum2 += alloc[i];
        }
        long fix = T - sum2;
        alloc[people - 1] += fix;

        // BigDecimal로 변환
        java.util.ArrayList<BigDecimal> out = new java.util.ArrayList<>();
        for (long v : alloc) out.add(BigDecimal.valueOf(v));
        return out;
    }

    // 기존 split 메서드는 더 이상 사용하지 않음 (AssignItems로 대체됨)

    private boolean isNonItemLine(String desc) {
        if (desc == null) return true;
        String s = desc.trim().toLowerCase();
        String[] bad = {
            "total","subtotal","tax","vat","tip","service","discount","change","payment","card","cash","amount","sum",
            "합계","총액","소계","세금","부가세","봉사료","할인","결제","카드","현금","잔돈","승인","결제금액","주문합계","총"
        };
        for (String b : bad) if (s.contains(b)) return true;
        return false;
    }

    // ReceiptItem 한 줄의 금액 계산 (totalPrice 우선, 없으면 unitPrice*quantity)
    private java.math.BigDecimal lineTotal(ReceiptItem it) {
        java.math.BigDecimal q = it.quantity() == null ? java.math.BigDecimal.ONE : it.quantity();
        java.math.BigDecimal u = it.unitPrice() == null ? java.math.BigDecimal.ZERO : it.unitPrice();
        java.math.BigDecimal t = it.totalPrice() == null ? java.math.BigDecimal.ZERO : it.totalPrice();
        if (t.signum() == 0) t = u.multiply(q);
        return t.setScale(0, java.math.RoundingMode.HALF_UP);
    }

    /** 아이템을 랜덤으로 셔플 → 0..people-1로 라운드로빈 배정 */
    public AssignItemsResponse assignItems(AssignItemsRequest req) {
        int people = req.people();
        if (people <= 0) throw new IllegalArgumentException("people must be >= 1");

        List<ReceiptItem> src = req.items() == null ? List.of() : req.items();
        int itemCount = src.size();

        // 1) 합계성/헤더 라인 제거 + 금액 0 라인 제거 + 안전 보정
        List<ReceiptItem> filtered = new ArrayList<>();
        for (ReceiptItem it : src) {
            String d = it == null ? null : it.description();
            if (isNonItemLine(d)) continue;
            java.math.BigDecimal lt = (it == null) ? java.math.BigDecimal.ZERO : lineTotal(it);
            if (lt.signum() <= 0) continue;
            filtered.add(new ReceiptItem(
                    d,
                    it.quantity() == null ? java.math.BigDecimal.ONE : it.quantity(),
                    it.unitPrice() == null ? java.math.BigDecimal.ZERO : it.unitPrice(),
                    lt
            ));
        }
        int assignedCount = filtered.size();

        // 2) 랜덤 셔플
        Random rnd = (req.seed() == null) ? new SecureRandom() : new Random(req.seed());
        Collections.shuffle(filtered, rnd);

        // 3) 라운드로빈 배정 (예: 4개/3명 -> 2/1/1)
        List<List<ReceiptItem>> buckets = new ArrayList<>();
        for (int i = 0; i < people; i++) buckets.add(new ArrayList<>());
        for (int i = 0; i < filtered.size(); i++) {
            buckets.get(i % people).add(filtered.get(i));
        }

        // 4) 카드별 합계/개수 + 참여자 매핑(닉네임)
        List<AssignmentCard> cards = new ArrayList<>();
        java.math.BigDecimal totalAssigned = java.math.BigDecimal.ZERO;

        // 참여자 준비: 요청 participants가 있으면 사용, 없거나 부족하면 A/B/C... 자동 라벨
        List<Participant> plist = new ArrayList<>();
        if (req.participants() != null) {
            plist.addAll(req.participants());
        }
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = plist.size(); i < people; i++) {
            String label = (i < alphabet.length()) ? String.valueOf(alphabet.charAt(i)) : ("Card-" + i);
            plist.add(new com.example.recommendation.dto.Participant(null, label));
        }

        for (int i = 0; i < people; i++) {
            List<ReceiptItem> li = buckets.get(i);
            java.math.BigDecimal sub = li.stream()
                    .map(ReceiptItem::totalPrice)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                    .setScale(0, java.math.RoundingMode.HALF_UP);
            totalAssigned = totalAssigned.add(sub);
            cards.add(new AssignmentCard(i, plist.get(i), li, sub, li.size()));
        }

        return new AssignItemsResponse(
                people,
                itemCount,
                assignedCount,
                totalAssigned,
                cards
        );
    }
}
