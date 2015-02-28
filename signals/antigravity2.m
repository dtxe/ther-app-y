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
gyro_data = data.data(gyro_idx,:);
gyro_t = str2double(data.textdata(gyro_idx,1));
gyro_t = gyro_t - gyro_t(1);


%% Resample
% Need to resample, since std-dev is +/- 20% of mean time difference

% find average sampling period, divide by 4 (increase sample rate by 4x)
avg_diff = mean(diff(accl_t));
avg_diff = round(avg_diff)/4;

% get ending time of accl and gyro recordings, whichever ends first
ending_time = min([accl_t(end), gyro_t(end)]);

% form time vector
data_re_t = (0:avg_diff:ending_time)';

data_re_srate = 1000/avg_diff;
data_re_len = length(data_re_t);


%%%% ACCELERATION
% create new time vector & interpolate
accl_re_data = interp1(accl_t, accl_data, data_re_t);

%%%% GYROSCOPE
% create new time vector & interpolate
gyro_re_data = interp1(gyro_t, gyro_data, data_re_t);


% Plot resampled things
if 0
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
    
    linkaxes(ax);
end

%% Filter

% get frequency vector corresponding to FFT
fq = linspace(0, data_re_srate/2, data_re_len);
fq_gain = ones(data_re_len, 1);

% frequency ranges to cut + invert for -ve freqs
fq_gain((fq < 0.02)) = 0;
fq_gain((fq > 30)) = 0;
fq_gain(data_re_len:-1:ceil(data_re_len/2)+1) = fq_gain(1:floor(data_re_len/2));

% filter using fft/ifft
accl_re_filtd = zeros(data_re_len, 3);
gyro_re_filtd = zeros(data_re_len, 3);
for kk = 1:3
    accl_re_filtd(:,kk) = real(ifft(fq_gain .* fft(accl_re_data(:,kk))));
    gyro_re_filtd(:,kk) = real(ifft(fq_gain .* fft(gyro_re_data(:,kk))));
end



% Plot filter stuff
if 0
    
    pltitle = {'X-axis Re Accl', 'Y-axis Re Accl', 'Z-axis Re Accl', 'X-axis Filt Accl', 'Y-axis Filt Accl', 'Z-axis Filt Accl',};

    figure;
    for kk = 1:3
        ax(kk) = subplot(2,3,kk);
        plot(data_re_t, accl_re_data(:,kk));
        ylabel('Accel (ms^{-2})');
        xlabel('Time (ms)');
        title(pltitle{kk});

        ax(kk+3) = subplot(2,3,kk+3);
        plot(data_re_t, accl_re_filtd(:,kk));
        ylabel('Accel (ms^-2)');
        xlabel('Time (ms)');
        title(pltitle{kk+3});
    end

    linkaxes(ax);

end


%% Integration

%%%%% Rotation matrix
rotatevec3d = @(x, rot) ([cos(rot(1))*cos(rot(3))-cos(rot(2))*sin(rot(1))*sin(rot(3)), -cos(rot(1))*sin(rot(3))-cos(rot(2))*sin(rot(1))*cos(rot(3)), sin(rot(1))*sin(rot(2));
                          sin(rot(1))*cos(rot(3))+cos(rot(2))*cos(rot(1))*sin(rot(3)), -sin(rot(1))*sin(rot(3))+cos(rot(2))*cos(rot(1))*cos(rot(3)), -cos(rot(1))*sin(rot(2));
                          sin(rot(3))*sin(rot(2)),                                     cos(rot(3))*sin(rot(2)),                                      cos(rot(2))            ] * x')';

%%%%% RAW ACCL
vel = zeros(data_re_len, 3);
for kk = 1:3
    % initial velocity is zero
    vel(1,kk) = accl_re_filtd(1,kk)*(avg_diff/1000);
    
    for jj = 2:data_re_len
        vel(jj,kk) = vel(jj-1,kk) + accl_re_filtd(jj,kk)*(avg_diff/1000);
    end
end

pos = zeros(data_re_len, 3);
for kk = 1:3
    pos(1,kk) = vel(1,kk)*(avg_diff/1000);
    
    for jj = 2:data_re_len
        pos(jj,kk) = pos(jj-1,kk) + vel(jj,kk)*(avg_diff/1000);
    end
end

%%%%% ROTATION CORRECTED
% Keep track of rotation vector
rot = zeros(data_re_len, 3);
for kk = 1:3
    % initial rotation is zero
    rot(1,kk) = gyro_re_filtd(1,kk)*(avg_diff/1000);
    
    for jj = 2:data_re_len
        rot(jj,kk) = rot(jj-1,kk) + gyro_re_filtd(jj,kk)*(avg_diff/1000);
    end
end

% Integrate with corrected direction
vel_rt = zeros(data_re_len, 3);

vel_rt(1,:) = accl_re_filtd(1,:)*(avg_diff/1000);
for jj = 2:data_re_len
    accl_rtcor = accl_re_filtd(jj,:)*(avg_diff/1000);
    accl_rtcor = rotatevec3d(accl_rtcor, rot(jj,:));
    
    vel_rt(jj,:) = vel_rt(jj-1,:) + accl_rtcor;
end


pos_rt = zeros(data_re_len, 3);
for kk = 1:3
    pos_rt(1,kk) = vel_rt(1,kk)*(avg_diff/1000);
    
    for jj = 2:data_re_len
        pos_rt(jj,kk) = pos_rt(jj-1,kk) + vel_rt(jj,kk)*(avg_diff/1000);
    end
end


% Plot integrated trace
figure;
plot3(pos(:,1), pos(:,2), pos(:,3));
daspect([1 1 1]);
title('Raw integrated trace');

figure;
plot3(pos(:,1), pos(:,2), pos(:,3));
daspect([1 1 1]);
title('Rotation-corrected integrated trace');


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

