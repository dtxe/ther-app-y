% Play around with rotations. Trying to debug why integrating with rotation
% doesn't change anything.
%
% Simeon Wong
% 2015 March 1

%%%%% Rotation matrix
tdsp_rotationmatrix;


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

%%
close all

tdsp_rotationmatrix;

testvector = [0 1 0];
rotvector = rotatevec3d(testvector, [pi/4 0 0]);

figure(7); clf(7);
hold on
quiver3(0, 0, 0, testvector(1), testvector(2), testvector(3), 'c', 'LineWidth', 5);
quiver3(0, 0, 0, rotvector(1), rotvector(2), rotvector(3), 'm', 'LineWidth', 5);

quiver3(0, 0, 0, 1, 0, 0, 'g');
quiver3(0, 0, 0, 0, 1, 0, 'b');
quiver3(0, 0, 0, 0, 0, 1, 'r');

xlabel('X');
ylabel('Y');
zlabel('Z');
daspect([1, 1, 1]);
