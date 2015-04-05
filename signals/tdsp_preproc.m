function [ out ] = tdsp_preproc( data, varargin )
%TDSP_PREPROC parse csv file with IMU data, zero reference time, sort by
%time values, remove samples with duplicate time stamps, resample data.
%
% Accepts an importData type struct in data.
% 
% Simeon Wong
% 2015 March 15

%% Parse function parameters
p = inputParser;
addParameter(p, 'plot', false);

parse(p, varargin{:});

%% Parse file
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
gyro_idx = cellfun(@(c) strcmp(c, 'g'), data.textdata(:,2));
gyro_len = sum(gyro_idx);
gyro_data = data.data(gyro_idx,:);
gyro_t = str2double(data.textdata(gyro_idx,1));
gyro_t = gyro_t - gyro_t(1);

[gyro_t, temp_idx] = sort(gyro_t);
gyro_data = gyro_data(temp_idx, :);

temp_idx = find(diff(gyro_t) <= 0)+1;
gyro_t(temp_idx) = [];
gyro_data(temp_idx, :) = [];


% rotation vector
rota_idx = cellfun(@(c) strcmp(c, 'r'), data.textdata(:,2));
rota_len = sum(rota_idx);
rota_data = data.data(rota_idx,:);
rota_t = str2double(data.textdata(rota_idx,1));
rota_t = rota_t - rota_t(1);

[rota_t, temp_idx] = sort(rota_t);
rota_data = rota_data(temp_idx, :);

temp_idx = find(diff(rota_t) <= 0)+1;
rota_t(temp_idx) = [];
rota_data(temp_idx, :) = [];



%% Axis correction
% CORRECT FOR STUPID !@#$%@$%^@%#$%^ ANDROID LEFT HAND RULE AXES
% Flip X direction
accl_data(:,1) = -1*accl_data(:,1);
gyro_data(:,1) = -1*gyro_data(:,1);

% Need to resample, since std-dev is +/- 20% of mean time difference

% find average sampling period, divide by 5 (increase sample rate by 5x)
avg_diff = mean(diff(accl_t));
avg_diff = round(avg_diff)/5;

% Guess the time division (either 10^3 or 10^9)
TIME_DIV = 10.^(round((log10(avg_diff)-3)/6)*6 + 3);

% get ending time of accl and gyro recordings, whichever ends first
ending_time = min([accl_t(end), gyro_t(end), rota_t(end)]);

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

%%%% ROTATION
% create new time vector & interpolate
rota_re_data = interp1(rota_t, rota_data, data_re_t);




% Plot resampled things
if p.Results.plot
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
        ylabel('Accel (ms^{-2})');
        xlabel('Time (ms)');
        title(pltitle{kk+3});
    end
    
    linkaxes(ax);
    
    
    figure;
    for kk = 1:3
        ax(kk) = subplot(2,3,kk);
        plot(gyro_t, gyro_data(:,kk));
        ylabel('Angular Accl (rad \cdot s^{-1})');
        xlabel('Time (ms)');
        title(pltitle{kk});

        ax(kk+3) = subplot(2,3,kk+3);
        plot(data_re_t, gyro_re_data(:,kk));
        ylabel('Angular Accl (rad \cdot s^{-1})');
        xlabel('Time (ms)');
        title(pltitle{kk+3});
    end
    
    linkaxes(ax);
    
    
    figure;
    for kk = 1:3
        ax(kk) = subplot(2,3,kk);
        plot(rota_t, rota_data(:,kk));
        ylabel('Angular Position (rad)');
        xlabel('Time (ms)');
        title(pltitle{kk});

        ax(kk+3) = subplot(2,3,kk+3);
        plot(data_re_t, rota_re_data(:,kk));
        ylabel('Angular Position (rad)');
        xlabel('Time (ms)');
        title(pltitle{kk+3});
    end
    
    linkaxes(ax);
end

%% output things
out.accl_t = accl_t;
out.accl_data = accl_data;
out.accl_re_data = accl_re_data;

out.gyro_t = gyro_t;
out.gyro_data = gyro_data;
out.gyro_re_data = gyro_re_data;

out.rota_t = rota_t;
out.rota_data = rota_data;
out.rota_re_data = rota_re_data;

out.data_re_t = data_re_t;
out.data_re_srate = data_re_srate;
out.data_re_dt = data_re_dt;


end

