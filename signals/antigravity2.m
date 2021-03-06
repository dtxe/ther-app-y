% Test various methods of dealing with gravity in accelerometer data
% Modified for recordings made using the therappy app
% Simeon Wong
% 2014 Feb 28

close all

% Parameters

FLAG_ANIMATE        = false;
FLAG_ANIMATE360     = false;
FLAG_PLOTRESAMPLE   = false;
FLAG_PLOTFILTER     = true;
FLAG_PLOTVELPOS     = true;
FLAG_PLOTTRACE      = true;

FLAG_LINEARCORRECT  = false;     % assume same start/end position. correct linearly through.
FLAG_VELCORRECT     = false;      % assume same start/end, correct based on absolute velocity.

FILENAME = 'therappy1429730667994';

slidewnd_len = 1500;


%% Timing
t_begin = tic;

%% Data Preprocessing

if ~exist('data', 'var')
    % Load data
    data = importdata(['./assets2/cindy-protocol/' FILENAME '.txt']);

    % Separate accelerometer and rotation
    % count number of a, r
    accl_idx = cellfun(@(c) strcmp(c, 'a'), data.textdata(:,2));
    accl_len = sum(accl_idx);

    % get accl data
    accl_t = str2double(data.textdata(accl_idx,1));
    accl_data = data.data(accl_idx,:);

    % zero-ref time vector
    accl_t = accl_t - accl_t(1);

    % ensure data is sorted
    [accl_t, temp_idx] = sort(accl_t);
    accl_data = accl_data(temp_idx, :);

    % check for duplicate values
    temp_idx = find(diff(accl_t) <= 0)+1;
    accl_t(temp_idx) = [];
    accl_data(temp_idx, :) = [];

    % gyroscope data
    % gyro_idx = cellfun(@(c) strcmp(c, 'r') || strcmp(c, 'b'), data.textdata(:,2));
    % gyro_len = sum(gyro_idx);
    % gyro_data = data.data(gyro_idx,:);
    % gyro_t = str2double(data.textdata(gyro_idx,1));
    % gyro_t = gyro_t - gyro_t(1);
    % 
    % [gyro_t, temp_idx] = sort(gyro_t);
    % gyro_data = gyro_data(temp_idx, :);
    % 
    % temp_idx = find(diff(gyro_t) <= 0)+1;
    % gyro_t(temp_idx) = [];
    % gyro_data(temp_idx, :) = [];

    % CORRECT FOR STUPID !@#$%@$%^@%#$%^ ANDROID LEFT HAND RULE AXES
    % Flip X direction
    % accl_data(:,1) = -1*accl_data(:,1);
    % gyro_data(:,1) = -1*gyro_data(:,1);
end


%% Resample
% Need to resample, since std-dev is +/- 20% of mean time difference

% find average sampling period, divide by 4 (increase sample rate by 4x)
avg_diff = mean(diff(accl_t));
avg_diff = round(avg_diff)/4;

% Guess the time division (either 10^3 or 10^9)
time_div = 10.^(round((log10(avg_diff)-3)/6)*6 + 3);

% get ending time of accl and gyro recordings, whichever ends first
% ending_time = min([accl_t(end), gyro_t(end)]);
ending_time = accl_t(end);

% form time vector
data_re_t = (0:avg_diff:ending_time)';

data_re_srate = time_div/avg_diff;
data_re_dt = avg_diff/time_div;
data_re_len = length(data_re_t);


%%%% ACCELERATION
% create new time vector & interpolate
accl_re_data = interp1(accl_t, accl_data, data_re_t);

%%%% GYROSCOPE
% create new time vector & interpolate
% gyro_re_data = interp1(gyro_t, gyro_data, data_re_t);


% Plot resampled things
if FLAG_PLOTRESAMPLE
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
fq = linspace(0, data_re_srate, data_re_len);
fq_gain = ones(data_re_len, 1);

% frequency ranges to cut + invert for -ve freqs
fq_gain(1) = 0;     % kill DC
fq_gain((fq > 50)) = 0;
fq_gain(data_re_len:-1:ceil(data_re_len/2)+1) = fq_gain(1:floor(data_re_len/2));

% filter using fft/ifft
accl_re_filtd = zeros(data_re_len, 3);
% gyro_re_filtd = zeros(data_re_len, 3);
for kk = 1:3
    accl_re_filtd(:,kk) = real(ifft(fq_gain .* fft(accl_re_data(:,kk))));
%     gyro_re_filtd(:,kk) = real(ifft(fq_gain .* fft(gyro_re_data(:,kk))));
end


% subtract sliding window
slidewnd = zeros(size(accl_re_filtd));

for kk = 1:3
    for tt = 1:size(data_re_t,1)
        wndtt = max([1,tt-slidewnd_len/2]):min([data_re_len,tt+slidewnd_len/2]);
        slidewnd(tt,kk) = accl_re_filtd(tt,kk) - mean(accl_re_filtd(wndtt,kk),1);
    end
end

accl_re_filtd = slidewnd;



% Plot filter stuff
if FLAG_PLOTFILTER
    
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
        ylabel('Accel (ms^{-2})');
        xlabel('Time (ms)');
        title(pltitle{kk+3});
    end

    linkaxes(ax);

end


%% Integration

%%%%% Load Rotation matrix
tdsp_rotationmatrix;

%%%%% RAW ACCL
vel = zeros(data_re_len, 3);
for kk = 1:3
    % initial velocity is zero
%     vel(1,kk) = accl_re_filtd(1,kk)*data_re_dt;
    vel(1,kk) = 0;
    
    for jj = 2:data_re_len
        vel(jj,kk) = vel(jj-1,kk) + accl_re_filtd(jj,kk)*data_re_dt;
    end
end

pos = zeros(data_re_len, 3);
for kk = 1:3
%     pos(1,kk) = vel(1,kk)*data_re_dt;
    pos(1,kk) = 0;
    
    for jj = 2:data_re_len
        pos(jj,kk) = pos(jj-1,kk) + vel(jj,kk)*data_re_dt;
    end
end

% %%%%% ROTATION CORRECTED
% % Keep track of rotation vector
% rot = zeros(data_re_len, 3);
% 
% % initial rotation is zero
% rot(1,:) = gyro_re_filtd(1,:)*data_re_dt;
% 
% for jj = 2:data_re_len
%     % need to rotate the axis back to extrinsic frame of reference first
%     intrinsic_d_rot = gyro_re_filtd(jj,:)*data_re_dt;
%     extrinsic_d_rot = rotatevec3d(intrinsic_d_rot, rot(jj-1, :));
%     
%     % integrate into current rotation
%     rot(jj,:) = rot(jj-1,:) + extrinsic_d_rot;
% end
% 
% 
% % Integrate with corrected direction
% vel_rt = zeros(data_re_len, 3);
% 
% vel_rt(1,:) = accl_re_filtd(1,:)*data_re_dt;
% for jj = 2:data_re_len
%     accl_rtcor = accl_re_filtd(jj,:)*data_re_dt;
%     accl_rtcor = rotatevec3d(accl_rtcor, rot(jj,:));
%     
%     vel_rt(jj,:) = vel_rt(jj-1,:) + accl_rtcor;
% end
% 
% pos_rt = zeros(data_re_len, 3);
% for kk = 1:3
%     pos_rt(1,kk) = vel_rt(1,kk)*data_re_dt;
%     
%     for jj = 2:data_re_len
%         pos_rt(jj,kk) = pos_rt(jj-1,kk) + vel_rt(jj,kk)*data_re_dt;
%     end
% end

if FLAG_LINEARCORRECT
    pos = pos - [linspace(pos(1,1), pos(end,1), data_re_len)', linspace(pos(1,2), pos(end,2), data_re_len)', linspace(pos(1,3), pos(end,3), data_re_len)'];
elseif FLAG_VELCORRECT
    % initialize correction dataset
    pos_corr = zeros(size(pos));
    
    % divide amount of position correction needed based on absolute
    % velocity
    for kk = 1:3
        pos_corr(:,kk) = abs(vel(:,kk)) .* ((pos(end,kk)-pos(1,kk)) ./ sum(abs(vel(:,kk))));
    end
    
    pos = pos - cumsum(pos_corr);
end

% Plot filter stuff
if FLAG_PLOTVELPOS
    
    pltitle = {'X-axis Velocity', 'Y-axis Velocity', 'Z-axis Velocity', 'X-axis Position', 'Y-axis Position', 'Z-axis Position',};

    figure;
    for kk = 1:3
        ax(kk) = subplot(2,3,kk);
        plot(data_re_t, vel(:,kk));
        ylabel('Velocity (ms^{-1})');
        xlabel('Time (ms)');
        title(pltitle{kk});

        ax(kk+3) = subplot(2,3,kk+3);
        plot(data_re_t, pos(:,kk));
        ylabel('Position (m)');
        xlabel('Time (ms)');
        title(pltitle{kk+3});
    end

    linkaxes(ax(1:3));
    linkaxes(ax(4:6));

end

ax = [];
% Plot integrated trace
if FLAG_PLOTTRACE
%     figure('Position', [50 50 1000 800]);
figure;
%     ax(1) = subplot(1, 2, 1);
    
    plot3(pos(:,1), pos(:,2), pos(:,3), 'LineWidth', 3);
    daspect([1 1 1]);
    title('Raw integrated trace');
    xlabel('X distance (m)');
    ylabel('Y distance (m)');
    zlabel('Z distance (m)');
    

%     subplot(1, 2, 2);
%     
%     plot3(pos_rt(:,1), pos_rt(:,2), pos_rt(:,3));
%     daspect([1 1 1]);
%     title('Rotation-corrected integrated trace');
%     xlabel('X distance (m)');
%     ylabel('Y distance (m)');
%     zlabel('Z distance (m)');
end

return;


%% read outputted filtered data
accl_re_filtd = load('assets2/therappy1428275413665-output.txt');
data_re_len = length(accl_re_filtd);
data_re_dt = accl_t(end) * 1e-9 / data_re_len;

vel = zeros(data_re_len, 3);
for kk = 1:3
    % initial velocity is zero
    vel(1,kk) = accl_re_filtd(1,kk)*data_re_dt;
    
    for jj = 2:data_re_len
        vel(jj,kk) = vel(jj-1,kk) + accl_re_filtd(jj,kk)*data_re_dt;
    end
end

pos = zeros(data_re_len, 3);
for kk = 1:3
    pos(1,kk) = vel(1,kk)*data_re_dt;
    
    for jj = 2:data_re_len
        pos(jj,kk) = pos(jj-1,kk) + vel(jj,kk)*data_re_dt;
    end
end


ax(2) = subplot(1, 2, 2);
plot3(pos(:,1), pos(:,2), pos(:,3));
daspect([1 1 1]);
title('Post-filtered integrated trace');
xlabel('X distance (m)');
ylabel('Y distance (m)');
zlabel('Z distance (m)');

linkprop(ax, {'CameraPosition', 'CameraUpVector'});

%% Timing
toc(t_begin);


%% Animate

if FLAG_ANIMATE
    vidwriter = VideoWriter(['output\' FILENAME '-trace.avi']);
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
end

% rotate the 3D trace around to help visualize
if FLAG_ANIMATE360
    vidwriter = VideoWriter(['output\' FILENAME '-rotatetrace.avi']);
    vidwriter.FrameRate = 10;
    open(vidwriter);
    
    f = figure();
    
    plot3(pos(:,1), pos(:,2), pos(:,3));
    daspect([1 1 1]);
    title('Raw integrated trace');
    xlabel('X distance (m)');
    ylabel('Y distance (m)');
    zlabel('Z distance (m)');
    
    % set up rotation stuff
    NFRAMES = 60;
    az_steps = linspace(0,360,NFRAMES)-38;
    el_steps = 14*sin(linspace(0,2*pi,NFRAMES))+14;
    
    axis vis3d
    
    for kk = 1:NFRAMES
        view(az_steps(kk), el_steps(kk));
        writeVideo(vidwriter, getframe(f));
    end
    
    close(vidwriter);
end

