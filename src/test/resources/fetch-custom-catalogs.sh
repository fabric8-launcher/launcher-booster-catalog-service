#!/usr/bin/env bash

TEST_DIR="${PWD}/repos/custom-catalogs"

rm -rf ${TEST_DIR}
mkdir -p ${TEST_DIR} > /dev/null 2>&1

# BEGIN gastaldi's custom boosters

cd "${TEST_DIR}"
git clone git@github.com:gastaldi/booster-catalog.git "${TEST_DIR}/gastaldi"
cd "${TEST_DIR}/gastaldi"
for branch in `git branch -a | grep remotes | grep -v HEAD | grep -v master `; do
   git branch --track ${branch#remotes/origin/} $branch
done

git bundle create gastaldi-booster-catalog.bundle --all
cp  gastaldi-booster-catalog.bundle ${TEST_DIR}
rm -rf "${TEST_DIR}/gastaldi"

# END gastaldi's custom boosters

