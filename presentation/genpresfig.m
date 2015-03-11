% generate figures for signals processing pipeline slides for presentation
%
% Simeon Wong
% 2015 Mar 11


%% Plots of signals
figure(50);

plot(accl_re_filtd, 'LineWidth', '2');

title('Recorded Acceleration');
ylabel('Acceleration (ms^{-2})');
xlabel('Samples');
