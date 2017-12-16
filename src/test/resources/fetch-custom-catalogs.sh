#!/usr/bin/env bash

TEST_DIR="${PWD}/repos/custom-catalogs"

rm -rf ${TEST_DIR}
mkdir -p ${TEST_DIR} > /dev/null 2>&1

cd "${TEST_DIR}"

git clone git@github.com:gastaldi/booster-catalog.git "${TEST_DIR}/gastaldi"
cd "${TEST_DIR}/gastaldi"
for branch in `git branch -a | grep remotes | grep -v HEAD | grep -v master `; do
   git branch --track ${branch#remotes/origin/} $branch
done

git bundle create gastaldi-booster-catalog.bundle --all
cp  gastaldi-booster-catalog.bundle ${TEST_DIR}
rm -rf "${TEST_DIR}/gastaldi"

cd "${TEST_DIR}"
git clone git@github.com:chirino/booster-catalog.git "${TEST_DIR}/chirino"
cd "${TEST_DIR}/chirino"
for branch in `git branch -a | grep remotes | grep -v HEAD | grep -v master `; do
   git branch --track ${branch#remotes/origin/} $branch
done

git bundle create chirino-booster-catalog.bundle --all
cp chirino-booster-catalog.bundle ${TEST_DIR}

rm -rf "${TEST_DIR}/chirino"