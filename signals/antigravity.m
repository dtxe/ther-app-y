% Test various methods of dealing with gravity in accelerometer data
% Simeon Wong
% 2014 Dec 29

clear
close all

%% Load
DATAFILE = 'square.csv';

% Load data file
dat = load(DATAFILE);

% col 1: sequence number
% col 2: time
% col 3-5: x, y, z

srate = 1/(mean(diff(dat(:,2)))/1000);

%% Filter
% Setup filter
[z,p,k] = butter(4, [0.001 20]/(srate/2));
btr_sos = zp2sos(z,p,k);

% Filter data
filtd = zeros(length(dat), 3);
for kk = 1:3
    filtd(:,kk) = filtfilt(btr_sos, 1, dat(:,kk+2));
end

pltitle = {'X-axis Raw Accl', 'Y-axis Raw Accl', 'Z-axis Raw Accl'};

figure;
for kk = 1:3
    ax(kk) = subplot(1,3,kk);
    plot(dat(:,2)-dat(1,2), dat(:,kk+2));
    ylabel('Accel (ms^{-2})');
    xlabel('Time (ms)');
    title(pltitle{kk});
    
%     ax(kk+3) = subplot(2,3,kk+3);
%     plot(dat(:,2), filtd(:,kk));
%     ylabel('Accel (ms^-2)');
%     xlabel('Time (ms)');
%     title(pltitle{kk+3});
end

% linkaxes(ax);

return;


%% Integrate

vel = zeros(length(dat), 3);
for kk = 1:3
    vel(1,kk) = filtd(1,kk)*(dat(2,2)-dat(1,2))/1000;
    for jj = 2:length(dat)
        vel(jj,kk) = vel(jj-1,kk) + filtd(jj,kk)*(dat(jj,2)-dat(jj-1,2))/1000;
    end
end


pos = zeros(length(dat), 3);
for kk = 1:3
    pos(1,kk) = vel(1,kk)*(dat(2,2)-dat(1,2));
    for jj = 2:length(dat)
        pos(jj,kk) = pos(jj-1,kk) + vel(jj,kk)*(dat(jj,2)-dat(jj-1,2))/1000;
    end
end


% Plot device movement
figure;
subplot(1,3,1);
plot(pos(:,1), pos(:,2));
title('x-y');
axis square

subplot(1,3,2);
plot(pos(:,2), pos(:,3));
title('y-z');
axis square

subplot(1,3,3);
plot(pos(:,1), pos(:,3));
title('x-z');
axis square


figure;
plot3(pos(:,1), pos(:,2), pos(:,3));

%% Animate
vidwriter = VideoWriter([DATAFILE '.avi']);
open(vidwriter);
f = figure(50);
for tt = 1:3:length(pos)
    hold on;
    plot3(pos(:,1), pos(:,2), pos(:,3));
    plot3(pos(1:tt,1), pos(1:tt,2), pos(1:tt,3), 'r', 'LineWidth', 3);
    
    view([49.5,-44]);
    refresh(f);
    
    writeVideo(vidwriter, getframe(f));
    clf(f);
end

close(vidwriter);
