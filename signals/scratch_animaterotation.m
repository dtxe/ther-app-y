% Animate the rotation of a phone throughout the recording to verify the
% rotational integration code.
%
% Simeon Wong
% 2015 March 1

%%%%% Rotation matrix
rotate3dX = @(theta) [1, 0, 0; 0, cos(theta), -1*sin(theta); 0, sin(theta), cos(theta)];
rotate3dY = @(theta) [cos(theta), 0, sin(theta); 0, 1, 0; -1*sin(theta), 0, cos(theta)];
rotate3dZ = @(theta) [cos(theta), -1*sin(theta), 0; sin(theta), cos(theta), 0; 0, 0, 1];
    
rotatevec3d = @(x, rot) (rotate3dZ(rot(3)) * rotate3dY(rot(2)) * rotate3dX(rot(1)) * x')';

%% Import image
ssimage = imread('./assets/therappy-menu-cropped.jpg');
ssimage = imresize(ssimage, 100/length(ssimage));           % resize to 500px
[ssimage, ssmap] = rgb2ind(ssimage, 8);

ss_size = size(ssimage);

% generate grid corresponding to image
[ss_x, ss_y] = meshgrid(ss_size(1):-1:1, 1:ss_size(2));

% scale grid to a size of length 1 and centre at origin
ss_x = (ss_x' / ss_size(1)) - 0.5;
ss_y = ((ss_y' - ss_size(2)/2) / ss_size(1));
ss_z = 0 * ones(size(ss_x));


%% Plotting
f = figure(50);
hold on

vidwriter = VideoWriter(['phonerotate.avi']);
open(vidwriter);

for kk = 1:5:length(rot)
    hold on
    
    % rotate the view matrix
    ss_x_tmp = zeros(size(ss_x));
    ss_y_tmp = zeros(size(ss_x));
    ss_z_tmp = zeros(size(ss_x));
    for jj = 1:numel(ss_x)
        outvec = rotatevec3d([ss_x(jj), ss_y(jj), ss_z(jj)], rot(kk,:));
        ss_x_tmp(jj) = outvec(1);
        ss_y_tmp(jj) = outvec(2);
        ss_z_tmp(jj) = outvec(3);
    end
    
    % plot picture
    surf(ss_x_tmp, ss_y_tmp, ss_z_tmp, double(ssimage), 'EdgeColor', 'none', 'FaceAlpha', 0.7);
    colormap(ssmap);
    daspect([1,1,1]);
    view(-115,40);

    set(gca, 'YDir', 'reverse');
    set(gca, 'XLim', [-0.75 0.75]);
    set(gca, 'YLim', [-0.75 0.75]);
    set(gca, 'ZLim', [-0.75 0.75]);


    % plot axis
    quiver3(0, 0, 0, 0.75, 0, 0, 'r');
    quiver3(0, 0, 0, 0, 0.75, 0, 'g');
    quiver3(0, 0, 0, 0, 0, 0.75, 'b');

    xlabel('X');
    ylabel('Y');
    zlabel('Z');
    
    % save video frame
    refresh(f);
    writeVideo(vidwriter, getframe(f));
    clf(f);
end
