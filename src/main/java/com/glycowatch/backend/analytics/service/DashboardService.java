package com.glycowatch.backend.analytics.service;

import com.glycowatch.backend.analytics.dto.ChartPointDto;
import com.glycowatch.backend.analytics.dto.DashboardResponseDto;
import java.util.List;

public interface DashboardService {

    DashboardResponseDto getDashboard(String authenticatedEmail);

    List<ChartPointDto> getChartData(String authenticatedEmail);
}

