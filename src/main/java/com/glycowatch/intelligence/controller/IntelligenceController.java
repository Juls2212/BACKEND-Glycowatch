package com.glycowatch.intelligence.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/intelligence")
@Tag(name = "Intelligence", description = "Intelligence endpoints")
public class IntelligenceController {
}
