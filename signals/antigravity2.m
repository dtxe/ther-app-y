% Test various methods of dealing with gravity in accelerometer data
% Modified for recordings made using the therappy app
% Simeon Wong
% 2014 Feb 28

close all
clear

data = importdata('./therappy1425147273141.txt');
% data = importdata('./therappy1424922398062.txt');

t_begin = tic;


%% Data Preprocessing
% Separate accelerometer and rotation
% count number of a, r
accl_idx = cellfun(@(c) strcmp(c, 'a'), data.textdata(:,2));
accl_len = sum(accl_idx);

% get accl data
accl_t = str2double(data.textdata(accl_idx,1));
accl_data = data.data(accl_idx,:);

% zero-ref time vector
accl_t = accl_t - accl_t(1);


% gyroscope data
gyro_idx = cellfun(@(c) strcmp(c, 'r'), data.textdata(:,2));
gyro_len = sum(gyro_idx);
gyro_t = str2double(data.textdata(gyro_idx,1));
gyro_data = data.data(gyro_idx,:);



%% Resample
% Need to resample, since std-dev is +/- 20% of mean time difference

% create new time vector
avg_accl_diff = mean(diff(accl_t));
avg_accl_diff = round(avg_accl_diff)/4;

accl_re_t = (0:avg_accl_diff:accl_t(end))';
accl_re_data = interp1(accl_t, accl_data, accl_re_t);

accl_re_srate = 1000/avg_accl_diff;
accl_re_len = length(accl_re_data);

% Plot resampled things
if 1
    pltitle = {'X-axis Raw Accl', 'Y-axis Raw Accl', 'Z-axis Raw Accl', 'X-axis Re Accl', 'Y-axis Re Accl', 'Z-axis Re Accl',};

    figure;
    for kk = 1:3
        ax(kk) = subplot(2,3,kk);
        plot(accl_t, accl_data(:,kk));
        ylabel('Accel (ms^{-2})');
        xlabel('Time (ms)');
        title(pltitle{kk});

        ax(kk+3) = subplot(2,3,kk+3);
        plot(accl_re_t, accl_re_data(:,kk));
        ylabel('Accel (ms^-2)');
        xlabel('Time (ms)');
        title(pltitle{kk+3});
    end
    
    linkaxes(ax);
end

%% Filter

FILTER_TYPE = 'fft';

% filter selection
if strcmp(FILTER_TYPE, 'iir')
    % Setup filter
    [z,p,k] = butter(4, [0.05 25]/(accl_re_srate/2), 'bandpass');
    btr_sos = zp2sos(z,p,k);

    % Filter data
    accl_re_filtd = zeros(accl_re_len, 3);
    for kk = 1:3
        accl_re_filtd(:,kk) = sosfilt(btr_sos, accl_re_data(:,kk));
    end
    
elseif strcmp(FILTER_TYPE, 'fft')
    % figure out what to cut
    fq = linspace(0, accl_re_srate/2, accl_re_len);
    fq_gain = ones(accl_re_len, 1);
    
    fq_gain((fq < 0.02)) = 0;
    fq_gain((fq > 30)) = 0;
    
    fq_gain(accl_re_len:-1:round(accl_re_len/2)+1) = fq_gain(1:round(accl_re_len/2));
    
    accl_re_filtd = zeros(accl_re_len, 3);
    for kk = 1:3
        accl_re_filtd(:,kk) = real(ifft(fq_gain .* fft(accl_re_data(:,kk))));
    end
end

pltitle = {'X-axis Re Accl', 'Y-axis Re Accl', 'Z-axis Re Accl', 'X-axis Filt Accl', 'Y-axis Filt Accl', 'Z-axis Filt Accl',};

figure;
for kk = 1:3
    ax(kk) = subplot(2,3,kk);
    plot(accl_re_t, accl_re_data(:,kk));
    ylabel('Accel (ms^{-2})');
    xlabel('Time (ms)');
    title(pltitle{kk});
    
    ax(kk+3) = subplot(2,3,kk+3);
    plot(accl_re_t, accl_re_filtd(:,kk));
    ylabel('Accel (ms^-2)');
    xlabel('Time (ms)');
    title(pltitle{kk+3});
end

linkaxes(ax);

% accl_data = filtd;


%% Integration

% accl_re_filtd(:,1) = accl_re_filtd(:,1) - mean(accl_re_filtd(:,1));
% accl_re_filtd(:,2) = accl_re_filtd(:,2) - mean(accl_re_filtd(:,2));
% accl_re_filtd(:,3) = accl_re_filtd(:,3) - mean(accl_re_filtd(:,3));


vel = zeros(accl_re_len, 3);
for kk = 1:3
    % initial velocity is zero
    vel(1,kk) = accl_re_filtd(1,kk)*(avg_accl_diff/1000);
    
    for jj = 2:accl_re_len
        vel(jj,kk) = vel(jj-1,kk) + accl_re_filtd(jj,kk)*(avg_accl_diff/1000);
    end
end

pos = zeros(accl_re_len, 3);
for kk = 1:3
    pos(1,kk) = vel(1,kk)*(avg_accl_diff/1000);
    
    for jj = 2:accl_re_len
        pos(jj,kk) = pos(jj-1,kk) + vel(jj,kk)*(avg_accl_diff/1000);
    end
end

figure;
plot3(pos(:,1), pos(:,2), pos(:,3));
daspect([1 1 1]);

%% Timing
toc(t_begin);

%% Animate
vidwriter = VideoWriter(['test.avi']);
open(vidwriter);
f = figure(50);
for tt = 1:20:length(pos)
    hold on;
    plot3(pos(:,1), pos(:,2), pos(:,3));
    plot3(pos(1:tt,1), pos(1:tt,2), pos(1:tt,3), 'r', 'LineWidth', 3);
    daspect([1 1 1]);
    
    refresh(f);
    
    writeVideo(vidwriter, getframe(f));
    clf(f);
end

close(vidwriter);

