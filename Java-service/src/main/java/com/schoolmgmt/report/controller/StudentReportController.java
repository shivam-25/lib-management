package com.schoolmgmt.report.controller;

import com.schoolmgmt.report.client.SchoolApiClient;
import com.schoolmgmt.report.model.Student;
import com.schoolmgmt.report.service.StudentPdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
public class StudentReportController {

    private final SchoolApiClient apiClient;
    private final StudentPdfService pdfService;

    public StudentReportController(SchoolApiClient apiClient, StudentPdfService pdfService) {
        this.apiClient = apiClient;
        this.pdfService = pdfService;
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> getStudentReport(@PathVariable String id) {
        Student student = apiClient.fetchStudent(id);
        byte[] pdf = pdfService.generate(student);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("student-" + id + "-report.pdf")
                .build());

        return new ResponseEntity<>(pdf, headers, 200);
    }
}
