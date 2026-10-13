package com.lqx.health_check_system.service;

import com.lqx.health_check_system.dto.ImportPreviewDTO;
import com.lqx.health_check_system.dto.ImportRequestDTO;
import com.lqx.health_check_system.dto.ImportResultDTO;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;

public interface HrService {

    void downloadTemplate(HttpServletResponse response);
    ImportPreviewDTO preview(MultipartFile file, Long doctorId);
    ImportResultDTO importByToken(String previewToken, ImportRequestDTO req);
}