figure;
plot2d3(x1)
a=gca() ;//get the current axes  
a.box="on";  
a.data_bounds=[0,0;720,26];

const=m1*ones(1,720);
plot(const)

figure;
plot2d3(x2)
a=gca() ;//get the current axes  
a.box="on";  
a.data_bounds=[0,0;720,26];

const=m2*ones(1,720);
plot(const)
