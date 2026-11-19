package com.yujing.note;

import com.yujing.common.api.ApiResponse;
import com.yujing.common.api.PageData;
import com.yujing.note.dto.NoteRequest;
import com.yujing.note.dto.NoteSummary;
import com.yujing.note.dto.NoteView;
import com.yujing.security.CurrentUserProvider;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteService noteService;
    private final CurrentUserProvider currentUserProvider;

    public NoteController(NoteService noteService, CurrentUserProvider currentUserProvider) {
        this.noteService = noteService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<PageData<NoteSummary>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(noteService.list(currentUserProvider.requireId(), keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<NoteView> get(@PathVariable Long id) {
        return ApiResponse.ok(noteService.get(currentUserProvider.requireId(), id));
    }

    @PostMapping
    public ApiResponse<NoteView> create(@RequestBody NoteRequest body) {
        return ApiResponse.ok(noteService.create(currentUserProvider.requireId(), body));
    }

    @PutMapping("/{id}")
    public ApiResponse<NoteView> update(@PathVariable Long id, @RequestBody NoteRequest body) {
        return ApiResponse.ok(noteService.update(currentUserProvider.requireId(), id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam long version) {
        noteService.delete(currentUserProvider.requireId(), id, version);
        return ApiResponse.ok();
    }
}
