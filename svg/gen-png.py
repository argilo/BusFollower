import os

for filename in os.listdir('svg'):
    if filename.endswith('.svg'):
        prefix = filename[0:-4]
        os.system('inkscape -e res/drawable-xhdpi/' + prefix + '.png -d 320 svg/' + prefix + '.svg')
        os.system('inkscape -e res/drawable-hdpi/' + prefix + '.png -d 240 svg/' + prefix + '.svg')
        os.system('inkscape -e res/drawable-mdpi/' + prefix + '.png -d 160 svg/' + prefix + '.svg')
        os.system('inkscape -e res/drawable-ldpi/' + prefix + '.png -d 120 svg/' + prefix + '.svg')
