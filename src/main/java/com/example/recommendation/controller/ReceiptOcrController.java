package com.example.recommendation.controller;

import com.example.recommendation.dto.AssignItemsRequest;
import com.example.recommendation.dto.AssignItemsResponse;
import com.example.recommendation.dto.Participant;
import com.example.recommendation.dto.ReceiptOcrRequest;
import com.example.recommendation.dto.ReceiptOcrResponse;
import com.example.recommendation.service.ReceiptOcrService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr/receipt")
public class ReceiptOcrController {

    private final ReceiptOcrService service;

    public ReceiptOcrController(ReceiptOcrService service) {
        this.service = service;
    }

    // (옵션) URL로 분석
    @PostMapping(value = "/url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ReceiptOcrResponse byUrl(@Valid @RequestBody ReceiptOcrRequest req) {
        return service.analyzeByUrl(req);
    }

    // 파일 업로드로 분석 (추천)
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReceiptOcrResponse byFile(@RequestPart("file") MultipartFile file) {
        return service.analyzeByFile(file);
    }

    @PostMapping(value = "/split", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssignItemsResponse split(@RequestBody AssignItemsRequest req) {
        return service.assignItems(req);
    }

    @PostMapping(value = "/file-assign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssignItemsResponse fileAssign(
            @RequestPart("file") MultipartFile file,
            @RequestParam("people") int people,
            @RequestParam(value = "seed", required = false) Long seed,
            @RequestParam(value = "participants", required = false) String participantsJson
    ) throws Exception {
        // 1) OCR
        var ocr = service.analyzeByFile(file);

        // 2) participants JSON(선택) 파싱
        java.util.List<Participant> participants = null;
        if (participantsJson != null && !participantsJson.isBlank()) {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            participants = java.util.Arrays.asList(om.readValue(participantsJson, Participant[].class));
        }

        // 3) 배정 요청 구성 후 서비스 호출
        var req = new AssignItemsRequest(people, seed, ocr.items(), participants);
        return service.assignItems(req);
    }
}
