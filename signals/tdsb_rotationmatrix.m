% Library script containing rotation matrix code used by other scripts
%
% Simeon Wong
% 2015 March 14

% Android coordinate axes
%   +x: points out the right side
%   +y: points out the top
%   +z: points out the front screen
%
% Rotation:
%   (+) direction is counter-clockwise, from the perspective of an observer
%   at some point in the +'ve of the axis, looking at device at origin.

rotate3dX = @(theta) [1, 0, 0; 0, cos(theta), -1*sin(theta); 0, sin(theta), cos(theta)];
rotate3dY = @(theta) [cos(theta), 0, sin(theta); 0, 1, 0; -1*sin(theta), 0, cos(theta)];
rotate3dZ = @(theta) [cos(theta), -1*sin(theta), 0; sin(theta), cos(theta), 0; 0, 0, 1];
    
rotatevec3d = @(x, rot) (rotate3dZ(rot(3)) * rotate3dY(rot(2)) * rotate3dX(rot(1)) * x')';
