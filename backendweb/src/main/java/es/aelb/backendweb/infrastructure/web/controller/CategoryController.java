package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.application.category.CreateCategoryCommand;
import es.aelb.backendweb.application.category.CreateCategoryUseCase;
import es.aelb.backendweb.application.category.DeleteCategoryUseCase;
import es.aelb.backendweb.application.category.UpdateCategoryCommand;
import es.aelb.backendweb.application.category.UpdateCategoryUseCase;
import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.CategoryRepository;
import es.aelb.backendweb.domain.category.CompetitionAgeCategory;
import es.aelb.backendweb.domain.category.valueobject.CategoryId;
import es.aelb.backendweb.infrastructure.web.dto.request.CreateCategoryRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.UpdateCategoryRequest;
import es.aelb.backendweb.infrastructure.web.dto.response.CategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;
    private final CategoryRepository    categoryRepository;

    public CategoryController(
            CreateCategoryUseCase createCategoryUseCase,
            UpdateCategoryUseCase updateCategoryUseCase,
            DeleteCategoryUseCase deleteCategoryUseCase,
            CategoryRepository categoryRepository
    ) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
        this.categoryRepository    = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll() {
        List<CategoryResponse> list = categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/age-categories")
    public ResponseEntity<List<es.aelb.backendweb.infrastructure.web.dto.response.CompetitionAgeCategoryResponse>> findAgeCategories() {
        return ResponseEntity.ok(java.util.Arrays.stream(CompetitionAgeCategory.values())
                .map(es.aelb.backendweb.infrastructure.web.dto.response.CompetitionAgeCategoryResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable String id) {
        return categoryRepository.findById(CategoryId.of(id))
                .map(CategoryResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest req) {
        Category created = createCategoryUseCase.execute(new CreateCategoryCommand(
                req.gender(), req.armSide(), req.weightLimit(), req.ageGroup(), req.ageCategory(), req.shift()
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId().value()).toUri();
        return ResponseEntity.created(location).body(CategoryResponse.from(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateCategoryRequest req
    ) {
        Category updated = updateCategoryUseCase.execute(new UpdateCategoryCommand(
                id, req.gender(), req.armSide(), req.weightLimit(), req.ageGroup(), req.ageCategory(), req.shift()
        ));
        return ResponseEntity.ok(CategoryResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteCategoryUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
