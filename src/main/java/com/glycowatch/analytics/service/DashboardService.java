package com.glycowatch.analytics.service;

import com.glycowatch.analytics.dto.ChartPointDto;
import com.glycowatch.analytics.dto.DashboardResponseDto;
import java.util.List;

public interface DashboardService {

    DashboardResponseDto getDashboard(String authenticatedEmail);

    List<ChartPointDto> getChartData(String authenticatedEmail);
}


