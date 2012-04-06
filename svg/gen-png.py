import os

for filename in os.listdir('svg'):
    if filename.endswith('.svg'):
        prefix = filename[0:-4]
        os.system('inkscape -e res/drawable-xhdpi/' + prefix + '.png -w 96 -h 96 svg/' + prefix + '.svg')
        os.system('inkscape -e res/drawable-hdpi/' + prefix + '.png -w 72 -h 72 svg/' + prefix + '.svg')
        os.system('inkscape -e res/drawable-mdpi/' + prefix + '.png -w 48 -h 48 svg/' + prefix + '.svg')
        os.system('inkscape -e res/drawable-ldpi/' + prefix + '.png -w 36 -h 36 svg/' + prefix + '.svg')
