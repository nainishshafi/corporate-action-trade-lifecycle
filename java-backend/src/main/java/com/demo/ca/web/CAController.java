package com.demo.ca.web;

import com.demo.ca.application.CALifecycleUseCase;
import com.demo.ca.application.CorporateActionView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ca")
public class CAController {

    private final CALifecycleUseCase lifecycleUseCase;

    public CAController(CALifecycleUseCase lifecycleUseCase) {
        this.lifecycleUseCase = lifecycleUseCase;
    }

    @GetMapping("/pending")
    public List<CorporateActionView> getPending() {
        return lifecycleUseCase.getPendingActions();
    }

    @PostMapping("/{id}/approve")
    public void approve(@PathVariable Long id) {
        lifecycleUseCase.approveAction(id);
    }
}
