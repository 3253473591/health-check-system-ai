package com.lqx.health_check_system.controller;

import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.dto.ImportPreviewDTO;
import com.lqx.health_check_system.dto.ImportRequestDTO;
import com.lqx.health_check_system.dto.ImportResultDTO;
import com.lqx.health_check_system.service.HrService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrService hrService;

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        hrService.downloadTemplate(response);
    }

    @PostMapping("/preview")
    public Result<ImportPreviewDTO> preview(
            @RequestParam("file")     MultipartFile file,
            @RequestParam("doctorId") Long doctorId) {
        ImportPreviewDTO dto = hrService.preview(file, doctorId);

        return Result.success(dto);
    }

    @PostMapping("/import")
    public Result<ImportResultDTO> importByToken(
            @RequestBody ImportRequestDTO req) {
        if (req.getPreviewToken() == null || req.getPreviewToken().isEmpty()) {
            return Result.error("previewToken 不能为空，请先调用 /preview");
        }
        ImportResultDTO result = hrService.importByToken(req.getPreviewToken(), req);
        return Result.success(result);
    }
}