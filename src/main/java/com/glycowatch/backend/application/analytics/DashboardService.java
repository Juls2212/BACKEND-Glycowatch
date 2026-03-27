package com.glycowatch.backend.application.analytics;

import com.glycowatch.backend.interfaces.dto.analytics.ChartPointDto;
import com.glycowatch.backend.interfaces.dto.analytics.DashboardResponseDto;
import java.util.List;

public interface DashboardService {

    DashboardResponseDto getDashboard(String authenticatedEmail);

    List<ChartPointDto> getChartData(String authenticatedEmail);
}

//dash