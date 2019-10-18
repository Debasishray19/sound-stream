 tic
% Control_points=20;
% imageframe = im2bw((getsnapshot(tonguedemo)),.2);
 %imageframe=im2bw(imread('sadmemory.jpeg'),.2);
 robot = java.awt.Robot();
pos = [15 130 930 515]; % [left top width height]
rect = java.awt.Rectangle(pos(1),pos(2),pos(3),pos(4));
cap = robot.createScreenCapture(rect);
% Convert to an RGB image
rgb = typecast(cap.getRGB(0,0,cap.getWidth,cap.getHeight,[],0,cap.getWidth),'uint8');
imgData = zeros(cap.getHeight,cap.getWidth,3,'uint8');
imgData(:,:,1) = reshape(rgb(3:4:end),cap.getWidth,[])';
imgData(:,:,2) = reshape(rgb(2:4:end),cap.getWidth,[])';
imgData(:,:,3) = reshape(rgb(1:4:end),cap.getWidth,[])';
% Show or save to file
imageframe=im2bw(imgData,.2);
se = strel('sphere',3);
imageframe=imerode(imageframe,se);
imshow(imageframe)
 im=(imageframe(:,10:30:size(imageframe,2)));
imshow(im)
 for i=1:size(im,2)
 Locations = find(islocalmin(im(:,i),'FlatSelection', 'first'));
 Area(i)=.05*abs(Locations(1,1)-Locations(2,1));
 %Area(i)=18*Area(i)/max(Area(i));
% Area = fliplr(Area);
 end
 toc
% figure,imshow(im)
%figure,plot(Area)