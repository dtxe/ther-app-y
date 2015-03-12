% generate figures for signals processing pipeline slides for presentation
%
% Simeon Wong
% 2015 Mar 11


%% Plots of signals
figure(50);
clf(50);

plot(accl_re_filtd, 'LineWidth', 2);

title('Recorded Acceleration');
ylabel('Acceleration (ms^{-2})');
xlabel('Samples');

xlim([1 1000]);


%% Create plot of transfer function
figure(50);
clf(50);

% generate arbitrary filter
b = firls(1000, [0 0.001 0.002 0.6 0.6002 1], [0 0 1 1 0 0]);

freqs(b, 1, logspace(-10, 100));

%% Plot position
figure(50);
clf(50);

plot(pos, 'LineWidth', 2);

%% Histogram
figure(50);
clf(50);

hist(diff(accl_t),20)

title('Period between acceleration samples');
xlabel('Period (ms)');
ylabel('Occurances');

%% Histogram gyro
clf(50);

hist(diff(gyro_t),20)

title('Period between gyroscope samples');
xlabel('Period (ms)');
ylabel('Occurances');


%% resampled data
    pltitle = {'X-axis Raw Accl', 'Y-axis Raw Accl', 'Z-axis Raw Accl', 'X-axis Re Accl', 'Y-axis Re Accl', 'Z-axis Re Accl',};

    figure;
    for kk = 1:3
        ax(kk) = subplot(2,3,kk);
        plot(accl_t, accl_data(:,kk));
        ylabel('Accel (ms^{-2})');
        xlabel('Time (ms)');
        title(pltitle{kk});

        ax(kk+3) = subplot(2,3,kk+3);
        plot(data_re_t, accl_re_data(:,kk));
        ylabel('Accel (ms^-2)');
        xlabel('Time (ms)');
        title(pltitle{kk+3});
    end
    
    linkaxes(ax([1,2,4,5]));
