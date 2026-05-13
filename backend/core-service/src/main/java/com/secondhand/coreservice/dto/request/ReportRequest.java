    package com.secondhand.coreservice.dto.request;

    import com.secondhand.coreservice.model.enums.ReportCode;
    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.NotNull;
    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Getter;
    import lombok.NoArgsConstructor;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class ReportRequest {

        @NotNull(message = "Mã báo cáo không được để trống")
        private ReportCode code;

        @NotBlank(message = "Lý do báo cáo không được để trống")
        private String reason;

        private String description;

        @NotBlank(message = "ID bài viết không được để trống")
        private String itemId;
    }
