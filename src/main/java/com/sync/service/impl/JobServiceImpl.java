package com.sync.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sync.mapper.JobConfigMapper;
import com.sync.mapper.JobMapper;
import com.sync.model.DatasourceConfig;
import com.sync.model.Job;
import com.sync.model.JobConfig;
import com.sync.service.DatasourceService;
import com.sync.service.JobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 同步任务服务实现 */
@Service
@Transactional(readOnly = true)
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements JobService {

    private final DatasourceService datasourceService;
    private final JobConfigMapper jobConfigMapper;

    public JobServiceImpl(DatasourceService datasourceService, JobConfigMapper jobConfigMapper) {
        this.datasourceService = datasourceService;
        this.jobConfigMapper = jobConfigMapper;
    }

    @Override
    public List<Job> findEnabledScheduledJobs() {
        return baseMapper.findEnabledScheduledJobs();
    }

    @Override
    public Job loadFullJob(Long jobId) {
        Job job = baseMapper.selectWithConfigs(jobId);
        if (job == null) return null;

        DatasourceConfig sourceDs = datasourceService.getById(job.getSourceDsId());
        DatasourceConfig targetDs = datasourceService.getById(job.getTargetDsId());
        job.setSourceDatasource(sourceDs);
        job.setTargetDatasource(targetDs);

        return job;
    }

    @Override
    public List<JobConfig> getConfigsByJobId(Long jobId) {
        return jobConfigMapper.findByJobId(jobId);
    }
}
