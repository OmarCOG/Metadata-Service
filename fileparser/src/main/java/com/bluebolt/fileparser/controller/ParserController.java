package com.bluebolt.fileparser.controller;

import com.bluebolt.fileparser.model.ParsedFile;
import com.bluebolt.fileparser.orchestrator.FileParserOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class ParserController {

    @Autowired
    private FileParserOrchestrator orchestrator;

    @PostMapping("/upload")
    public ParsedFile upload(@RequestParam("file") MultipartFile file) {
        return orchestrator.parseFile(file);
    }
}
