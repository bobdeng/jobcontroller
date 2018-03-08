package cn.bobdeng.job.domain.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor(staticName = "of")
public class StepInterceptor {

    @NonNull
    private String jobId;
    @NonNull
    private String jobStepId;

    private String arbitraryServer;


}
