FILENAME = 'therappy1428275413665';
data = importdata(['./assets2/' FILENAME '.txt']);

load('assets2/therappy1428275413665-output.txt');

figure;
ax(1) = subplot(2, 1, 1);
plot(data.data);
title('Raw');

ax(2) = subplot(2, 1, 2);
plot(therappy1428275413665_output);
title('Resampled, Filtered');


%%
FILENAME = 'therappy1428275413665';
data = importdata(['./assets2/' FILENAME '.txt']);

accl_idx = cellfun(@(c) strcmp(c, 'a'), data.textdata(:,2));
accl_len = sum(accl_idx);

% get accl data
accl_t = str2double(data.textdata(accl_idx,1));
accl_data = data.data(accl_idx,:);

% zero-ref time vector
accl_t = accl_t - accl_t(1);


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


plot3(pos(:,1), pos(:,2), pos(:,3));


%% animate



%%
pos = load('assets2/therappy1428280195403-output.txt');
figure; plot3(pos(:,1), pos(:,2), pos(:,3));

