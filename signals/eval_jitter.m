% Evaluate the jitter in acceleration and gyroscope values
% Simeon Wong
% 2015 March 1

testdata = accl_t/1000000;

meandiff = mean(diff(testdata));
stddiff = std(diff(testdata));

%% Show stats
fprintf('Mean difference: %.4f\n', meandiff);
fprintf('Std of difference: %.4f\n', stddiff);
fprintf('Std as prc of mean: %.4f%%\n', stddiff/meandiff*100);

%% Generate histogram
figure;
hist(diff(testdata), 20);

title('Histogram: time difference between subsequent measurements');
xlabel('Time differences');
ylabel('No. of occurances');
