% Test various methods of dealing with gravity in accelerometer data
% Modified for recordings made using the therappy app
% Simeon Wong
% 2014 Feb 28


data = importdata('./therappy1424922398062.txt');

%% Data Preprocessing
% Separate accelerometer and rotation
% count number of a, r
accl_idx = cellfun(@(c) c == 'a', data.textdata(:,2));
accl_len = sum(accl_idx);

% get accl data
accl_t = str2double(data.textdata(accl_idx,1));
accl_data = data.data(accl_idx,:);

% zero-ref time vector
accl_t = accl_t - accl_t(1);


%% Filter


srate = 1000/mean(diff(accl_t));

% Setup filter
[z,p,k] = butter(4, [1 10]/(srate/2));
btr_sos = zp2sos(z,p,k);

% Filter data
filtd = zeros(accl_len, 3);
for kk = 1:3
    filtd(:,kk) = filtfilt(btr_sos, 1, accl_data(:,kk));
end

pltitle = {'X-axis Raw Accl', 'Y-axis Raw Accl', 'Z-axis Raw Accl', 'X-axis Filt Accl', 'Y-axis Filt Accl', 'Z-axis Filt Accl',};

figure;
for kk = 1:3
    ax(kk) = subplot(2,3,kk);
    plot(accl_t, accl_data(:,kk));
    ylabel('Accel (ms^{-2})');
    xlabel('Time (ms)');
    title(pltitle{kk});
    
    ax(kk+3) = subplot(2,3,kk+3);
    plot(accl_t, filtd(:,kk));
    ylabel('Accel (ms^-2)');
    xlabel('Time (ms)');
    title(pltitle{kk+3});
end

accl_data = filtd;


%% Integration
vel = zeros(accl_len, 3);
for kk = 1:3
    % initial velocity is zero
    vel(1,kk) = accl_data(1,kk)*(accl_t(2) - accl_t(1))/1000;
    
    for jj = 2:accl_len
        vel(jj,kk) = vel(jj-1,kk) + accl_data(jj,kk)*(accl_t(jj)-accl_t(jj-1))/1000;
    end
end

pos = zeros(accl_len, 3);
for kk = 1:3
    pos(1,kk) = vel(1,kk)*(accl_t(2) - accl_t(1))/1000;
    
    for jj = 2:accl_len
        pos(jj,kk) = pos(jj-1,kk) + vel(jj,kk)*(accl_t(2) - accl_t(1))/1000;
    end
end

figure;
plot3(pos(:,1), pos(:,2), pos(:,3));



