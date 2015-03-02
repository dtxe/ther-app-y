% Play around with rotations. Trying to debug why integrating with rotation
% doesn't change anything.
%
% Simeon Wong
% 2015 March 1

%%%%% Rotation matrix
rotate3dX = @(theta) [1, 0, 0; 0, cos(theta), -1*sin(theta); 0, sin(theta), cos(theta)];
rotate3dY = @(theta) [cos(theta), 0, sin(theta); 0, 1, 0; -1*sin(theta), 0, cos(theta)];
rotate3dZ = @(theta) [cos(theta), -1*sin(theta), 0; sin(theta), cos(theta), 0; 0, 0, 1];
    
rotatevec3d = @(x, rot) (rotate3dZ(rot(3)) * rotate3dY(rot(2)) * rotate3dX(rot(1)) * x')';


%%%%% Parameters %%%%%
vec = [1/sqrt(2), 1/sqrt(2), 0;
       1, 0, 0;
       0, 1/sqrt(2), 1/sqrt(2);];
rot = [pi/2, pi/2, 0];
clrs = {'k'; 'c'; 'm';};
%%%%%%%%%%%%%%%%%%%%%%


%%%%% Visualization
rotvec = zeros(size(vec));
for kk = 1:size(vec, 1);
    rotvec(kk,:) = rotatevec3d(vec(kk,:), rot);
end

ax = [];

figure;
ax(1) = subplot(1, 2, 1);
hold on

for kk = 1:size(vec,1)
    quiver3(0, 0, 0, vec(kk,1), vec(kk,2), vec(kk,3), clrs{kk}, 'LineWidth', 5);
end
quiver3(0, 0, 0, 1, 0, 0, 'r');
quiver3(0, 0, 0, 0, 1, 0, 'g');
quiver3(0, 0, 0, 0, 0, 1, 'b');

title('Original');
xlabel('X');
ylabel('Y');
zlabel('Z');
daspect([1, 1, 1]);



ax(2) = subplot(1, 2, 2);
hold on

for kk = 1:size(vec,1)
    quiver3(0, 0, 0, rotvec(kk,1), rotvec(kk,2), rotvec(kk,3), clrs{kk}, 'LineWidth', 5);
end
quiver3(0, 0, 0, 1, 0, 0, 'r');
quiver3(0, 0, 0, 0, 1, 0, 'g');
quiver3(0, 0, 0, 0, 0, 1, 'b');

linkprop(ax, 'CameraPosition', 'CameraUpVector', 'XLim', 'YLim', 'ZLim');

title('Transformed');
xlabel('X');
ylabel('Y');
zlabel('Z');
daspect([1, 1, 1]);
