package com.example.recommendation.controller;

import com.example.recommendation.dto.AssignItemsRequest;
import com.example.recommendation.dto.AssignItemsResponse;
import com.example.recommendation.dto.Participant;
import com.example.recommendation.dto.ReceiptOcrRequest;
import com.example.recommendation.dto.ReceiptOcrResponse;
import com.example.recommendation.service.ReceiptOcrService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/ocr/receipt")
public class ReceiptOcrController {

    private final ReceiptOcrService service;
    
    // 메모리에 결과 저장 (임시 저장소)
    private final Map<String, AssignItemsResponse> resultStorage = new ConcurrentHashMap<>();

    public ReceiptOcrController(ReceiptOcrService service) {
        this.service = service;
    }

    // 1. URL로 영수증 분석 + 분배 (완전한 플로우)
    @PostMapping(value = "/url-assign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> urlAssign(
            @Valid @RequestBody ReceiptOcrRequest urlRequest,
            @RequestParam("people") int people,
            @RequestParam(value = "seed", required = false) Long seed,
            @RequestParam(value = "participants", required = false) String participantsJson
    ) throws Exception {
        // 1) OCR 분석
        var ocrResult = service.analyzeByUrl(urlRequest);
        
        // 2) participants JSON 파싱
        java.util.List<Participant> participants = parseParticipants(participantsJson);
        
        // 3) 분배 실행
        var splitResult = service.assignItems(new AssignItemsRequest(people, seed, ocrResult.items(), participants));
        
        // 4) 결과 저장
        String resultId = UUID.randomUUID().toString();
        resultStorage.put(resultId, splitResult);
        
        return ResponseEntity.ok(Map.of("resultId", resultId));
    }

    // 2. 파일 업로드로 영수증 분석 + 분배 (완전한 플로우)
    @PostMapping(value = "/file-assign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> fileAssign(
            @RequestPart("file") MultipartFile file,
            @RequestParam("people") int people,
            @RequestParam(value = "seed", required = false) Long seed,
            @RequestParam(value = "participants", required = false) String participantsJson
    ) throws Exception {
        // 1) OCR 분석
        var ocrResult = service.analyzeByFile(file);

        // 2) participants JSON 파싱
        java.util.List<Participant> participants = parseParticipants(participantsJson);

        // 3) 분배 실행
        var splitResult = service.assignItems(new AssignItemsRequest(people, seed, ocrResult.items(), participants));
        
        // 4) 결과 저장
        String resultId = UUID.randomUUID().toString();
        resultStorage.put(resultId, splitResult);

        return ResponseEntity.ok(Map.of("resultId", resultId));
    }

    // 3. 분배 결과 조회 (GET)
    @GetMapping("/result/{resultId}")
    public ResponseEntity<AssignItemsResponse> getResult(@PathVariable String resultId) {
        AssignItemsResponse result = resultStorage.get(resultId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    // === 기존 API (하위 호환성) ===

    // URL로 분석만
    @PostMapping(value = "/url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ReceiptOcrResponse byUrl(@Valid @RequestBody ReceiptOcrRequest req) {
        return service.analyzeByUrl(req);
    }

    // 파일로 분석만
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReceiptOcrResponse byFile(@RequestPart("file") MultipartFile file) {
        return service.analyzeByFile(file);
    }

    // 분배만
    @PostMapping(value = "/split", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssignItemsResponse split(@RequestBody AssignItemsRequest req) {
        return service.assignItems(req);
    }

    // === 유틸리티 메서드 ===

    private java.util.List<Participant> parseParticipants(String participantsJson) {
        if (participantsJson == null || participantsJson.isBlank()) {
            return null;
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            return java.util.Arrays.asList(om.readValue(participantsJson, Participant[].class));
        } catch (Exception e) {
            return null;
        }
    }
}