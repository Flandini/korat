myobserved = [1.572 4.198 16.679 76.453 2.782 8.033 297.752 1.6 37.41 1.857 9.81 71.784 10.393 380.292 3.146 28.615 1.589 5.277 27.626 156.787 3.713 28.592 245.118 2.491 13.3 88.94];
paperobserved = [0.9 3.2 11.8 50.5 0.5 2.4 21.2 0.7 23.0 0.9 5.9 43.5 5.3 216.7 1.9 19.6 0.8 3.9 20.7 116.1 2.4 20.4 176.4 1.3 8.5 63.2];
categories = {'BT10','BT11', 'BT12', 'BH7', 'BH8', 'BH9', 'DS5', 'DS6', 'DLL10', 'DLL11', 'DLL12', 'FH6', 'FH7','HA8', 'HA9','RBT8', 'RBT9', 'RBT10', 'RBT11', 'ST8', 'ST9','SLL10', 'SLL11', 'SLL12'};

figure

%bar(myobserved)

scatter(1:26, myobserved, 'filled');
set(gca, 'XTick', 1:26, 'XTickLabel', categories);
set(gca, 'XTickLabelRotation', 90)
set(gcf, 'PaperOrientation', 'landscape');
ylabel("Time (sec)");
xlabel("Run label of the form DATASTRUCTURE:#ARGS");
title("Time in seconds per data structure and size");
legend('My observed times');

hold on
scatter(1:26, paperobserved, 'filled');
legend('My observed times', 'Paper observed times');

set(gcf,'Position',[50 50 1200 800]);
set(gcf,'PaperPositionMode','auto');

%print(gcf, '-dpdf', 'test.pdf');

close all

%%

logratio = log10(myobserved ./ paperobserved);
scatter(1:26, logratio)
set(gca, 'XTick', 1:26, 'XTickLabel', categories)
ylabel("Log10 ratio of (my observed data time / paper data time)");
xlabel("Run label of the form DATASTRUCTURE:#ARGS");
title("Log10 Ratio of observed times to paper observed times by data structure and size");
