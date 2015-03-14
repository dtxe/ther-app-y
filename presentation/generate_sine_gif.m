% generate sine wave gif animation for signals processing loading screen
%
% Simeon Wong
% 2015 March 10

NUM_WINDOWS  = 150;
WND_LENGTH   = 250;
NUM_SAMPLES  = 1000;

t = linspace(0, 4*pi, NUM_SAMPLES);

x1 = sin(30*t);
x2 = sin(2*t);
x3 = sin(5*t);
x4 = sin(7*t);

xout = x1 .* x2 .* x3 .* x4;

f = figure(50);
set(f, 'Color', [0 0 0], 'Position', [100 100 800 400]);
plot(xout, 'Color', 'white', 'LineWidth', 3);
axis off


for kk = 1:NUM_WINDOWS
    wnd_start = (kk-1)*(NUM_SAMPLES/2)/NUM_WINDOWS;
    xlim(wnd_start + [0, WND_LENGTH-1]);
    ylim([-0.6 0.6]);
    
    frame = getframe(f);
    im = frame2im(frame);
    [imind,cm] = rgb2ind(im,2);
    
    if kk == 1
        imwrite(imind, cm,'loading.gif','gif', 'Loopcount',inf,'DelayTime',1/40, 'TransparentColor', 0);
    else
        imwrite(imind, cm,'loading.gif','gif', 'WriteMode','append','DelayTime',1/40, 'TransparentColor', 0);
    end
end
