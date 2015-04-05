% Test various methods of dealing with gravity in accelerometer data
% Modified for recordings made using the therappy app
%
% re-basis the acceleration vector before filtering
% using the rotation vector
%
% Simeon Wong
% 2014 March 26

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
FILENAME = 'therappy1427399900469';
data = importdata(['./assets/' FILENAME '.txt']);

%% Preprocessing
preproc_data = tdsp_preproc(data);

%% Rotate the acceleration vector

% load rotation matrix
tdsp_rotationmatrix;

accl_rebased = zeros(size(preproc_data.accl_re_data));
for kk = 1:length(preproc_data.data_re_t)
    accl_rebased(kk,:) = rotatevec3d(preproc_data.accl_re_data(kk,:), preproc_data.rota_re_data(kk,:));
end

