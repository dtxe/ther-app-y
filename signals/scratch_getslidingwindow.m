
slidewnd_len = 1000;

slidewnd = zeros(size(accl_re_filtd));

for kk = 1:3
    for tt = 1:size(data_re_t,1)
        wndtt = max([1,tt-slidewnd_len/2]):min([data_re_len,tt+slidewnd_len/2]);
        slidewnd(tt,kk) = accl_re_filtd(tt,kk) - mean(accl_re_filtd(wndtt,kk),1);
    end
end

pltitle = {'X-axis Raw Accl', 'Y-axis Raw Accl', 'Z-axis Raw Accl', 'X-axis Slide Window', 'Y-axis Slide Window', 'Z-axis Slide Window',};

ax = [];
figure;
for kk = 1:3
    ax(kk) = subplot(2,3,kk);
    plot(data_re_t, accl_re_filtd(:,kk));
    ylabel('Accel (ms^{-2})');
    xlabel('Time (ms)');
    title(pltitle{kk});

    ax(kk+3) = subplot(2,3,kk+3);
    plot(data_re_t, slidewnd(:,kk));
    ylabel('Accel (ms^-2)');
    xlabel('Time (ms)');
    title(pltitle{kk+3});
end
linkaxes(ax);
