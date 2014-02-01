

rm -rf .git
git init
git add .
git commit -m "Initial commit"

git remote add origin https://github.com/dkirrane/ggitflow-maven-plugin.git
git push -u --force origin master
