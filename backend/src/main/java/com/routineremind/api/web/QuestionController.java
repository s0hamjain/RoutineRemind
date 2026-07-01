package com.routineremind.api.web;

import com.routineremind.api.model.MediaUpload;
import com.routineremind.api.model.Question;
import com.routineremind.api.model.QuestionResponse;
import com.routineremind.api.security.AuthUser;
import com.routineremind.api.security.CurrentUser;
import com.routineremind.api.service.QuestionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/schedules/{scheduleId}/questions")
    public List<Question> questions(@CurrentUser AuthUser authUser, @PathVariable String scheduleId) {
        return questionService.listQuestions(authUser.uid(), scheduleId);
    }

    @PostMapping("/schedules/{scheduleId}/questions")
    public Question createQuestion(@CurrentUser AuthUser authUser,
                                   @PathVariable String scheduleId,
                                   @Valid @RequestBody QuestionRequest request) {
        return questionService.createQuestion(
                authUser.uid(),
                scheduleId,
                request.prompt(),
                request.type(),
                request.order()
        );
    }

    @GetMapping("/questions/{questionId}/responses")
    public List<QuestionResponse> responses(@CurrentUser AuthUser authUser, @PathVariable String questionId) {
        return questionService.listResponses(authUser.uid(), questionId);
    }

    @PostMapping("/questions/{questionId}/responses")
    public QuestionResponse submitResponse(@CurrentUser AuthUser authUser,
                                           @PathVariable String questionId,
                                           @Valid @RequestBody TextResponseRequest request) {
        return questionService.submitTextResponse(authUser.uid(), questionId, request.text());
    }

    @PostMapping("/questions/{questionId}/responses/media")
    public MediaUpload createMediaUpload(@CurrentUser AuthUser authUser,
                                         @PathVariable String questionId,
                                         @Valid @RequestBody MediaUploadRequest request) {
        return questionService.createMediaUpload(authUser.uid(), questionId, request.contentType());
    }

    @PostMapping("/responses/{responseId}/transcribe")
    public QuestionResponse transcribe(@CurrentUser AuthUser authUser,
                                       @PathVariable String responseId,
                                       @RequestBody(required = false) TranscribeRequest request) {
        return questionService.transcribeResponse(
                authUser.uid(),
                responseId,
                request == null ? null : request.languageCode()
        );
    }

    public record QuestionRequest(@NotBlank String prompt, String type, Integer order) {
    }

    public record TextResponseRequest(@NotBlank String text) {
    }

    public record MediaUploadRequest(@NotBlank String contentType) {
    }

    public record TranscribeRequest(String languageCode) {
    }
}
