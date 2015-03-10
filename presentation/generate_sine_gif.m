% generate sine wave gif animation for signals processing loading screen
%
% Simeon Wong
% 2015 March 10

NUM_WINDOWS  = 30;
WND_LENGTH   = 100;
NUM_SAMPLES  = 1000;

t = linspace(0,2*pi, NUM_SAMPLES);

x1 = sin(50*t);
x2 = sin(2*t);
x3 = sin(5*t);
x4 = sin(7*t);

xout = x1 .* x2 .* x3 .* x4;

f = figure(50);
set(f, 'Color', [0.176,0.02,0.294]);
plot(xout, 'Color', 'white', 'LineWidth', 3);
axis off


for kk = 1:NUM_WINDOWS
    wnd_start = (kk-1)*(NUM_SAMPLES-WND_LENGTH);
end
