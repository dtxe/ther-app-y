% Test various methods of dealing with gravity in accelerometer data
% Modified for recordings made using the therappy app
%
% re-basis the acceleration vector before filtering
%
% Simeon Wong
% 2014 Feb 28

close all
clear

% Parameters
% TIME_DIV = 1000 * 1000 * 1000;      % ns
TIME_DIV = 1000;                % ms

FLAG_ANIMATE        = false;
FLAG_ANIMATE360     = false;
FLAG_PLOTRESAMPLE   = false;
FLAG_PLOTFILTER     = true;
FLAG_PLOTTRACE      = true;

FLAG_LINEARCORRECT  = false;     % assume same start/end position. correct linearly through.
FLAG_VELCORRECT     = false;      % assume same start/end, correct based on absolute velocity.

% Load data
FILENAME = 'therappy1427388439057';
data = importdata(['./assets/' FILENAME '.txt']);


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

% ensure data is sorted
[accl_t, temp_idx] = sort(accl_t);
accl_data = accl_data(temp_idx, :);

% check for duplicate values
temp_idx = find(diff(accl_t) <= 0)+1;
accl_t(temp_idx) = [];
accl_data(temp_idx, :) = [];

% gyroscope data
gyro_idx = cellfun(@(c) strcmp(c, 'r') || strcmp(c, 'g'), data.textdata(:,2));
gyro_len = sum(gyro_idx);
gyro_data = data.data(gyro_idx,:);
gyro_t = str2double(data.textdata(gyro_idx,1));
gyro_t = gyro_t - gyro_t(1);

[gyro_t, temp_idx] = sort(gyro_t);
gyro_data = gyro_data(temp_idx, :);

temp_idx = find(diff(gyro_t) <= 0)+1;
gyro_t(temp_idx) = [];
gyro_data(temp_idx, :) = [];

% CORRECT FOR STUPID !@#$%@$%^@%#$%^ ANDROID LEFT HAND RULE AXES
% Flip X direction
% accl_data(:,1) = -1*accl_data(:,1);
% gyro_data(:,1) = -1*gyro_data(:,1);




%% Resample
% Need to resample, since std-dev is +/- 20% of mean time difference

% find average sampling period, divide by 4 (increase sample rate by 4x)
avg_diff = mean(diff(accl_t));
avg_diff = round(avg_diff)/4;

% get ending time of accl and gyro recordings, whichever ends first
ending_time = min([accl_t(end), gyro_t(end)]);

% form time vector
data_re_t = (0:avg_diff:ending_time)';

data_re_srate = TIME_DIV/avg_diff;
data_re_dt = avg_diff/TIME_DIV;
data_re_len = length(data_re_t);


%%%% ACCELERATION
% create new time vector & interpolate
accl_re_data = interp1(accl_t, accl_data, data_re_t);

%%%% GYROSCOPE
% create new time vector & interpolate
gyro_re_data = interp1(gyro_t, gyro_data, data_re_t);


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

%% Integrate rotation

%%%%% Load Rotation matrix
tdsp_rotationmatrix;

%%%%% ROTATION CORRECTED

% use 2 second resting at beginning to calibrate



% Keep track of rotation vector
rot = zeros(data_re_len, 3);



% initial rotation is zero
rot(1,:) = gyro_re_data(1,:)*data_re_dt;

for jj = 2:data_re_len
    % need to rotate the axis back to extrinsic frame of reference first
    intrinsic_d_rot = gyro_re_data(jj,:)*data_re_dt;
    extrinsic_d_rot = rotatevec3d(intrinsic_d_rot, rot(jj-1, :));
    
    % integrate into current rotation
    rot(jj,:) = rot(jj-1,:) + extrinsic_d_rot;
end





