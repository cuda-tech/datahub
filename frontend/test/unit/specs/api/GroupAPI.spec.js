import groupApi from '../../../../src/api/GroupAPI'
import axios from 'axios';

jest.mock('axios');

describe('项目组接口', () => {

    test('通过 ID 查找项目组', done => {
        axios.get.mockResolvedValue({
            group: {
                id: 2,
                name: 'testUpdate',
                createTime: '2029-05-26 23:17:01',
                updateTime: '2020-05-23 12:36:21'
            }
        });

        groupApi.find(2, group => {
            expect(group).toEqual({
                id: 2,
                name: 'testUpdate',
                createTime: '2029-05-26 23:17:01',
                updateTime: '2020-05-23 12:36:21'
            });
            done()
        })
    });

    test('创建项目组', done => {
        axios.post.mockResolvedValue({
            group: {
                id: 40,
                name: 'testCreate',
                createTime: '2020-05-23 12:36:52',
                updateTime: '2020-05-23 12:36:52'
            }
        });

        groupApi.create('testCreate', group => {
            expect(group).toEqual({
                id: 40,
                name: 'testCreate',
                createTime: '2020-05-23 12:36:52',
                updateTime: '2020-05-23 12:36:52'
            });
            done()
        })
    });

    test('删除项目组', done => {
        axios.delete.mockResolvedValue();
        groupApi.delete(1, () => done())
    });

    test('项目组分页查询', done => {
        axios.get.mockResolvedValue({
            groups: [
                {
                    id: 2,
                    name: 'testUpdate',
                    createTime: '2029-05-26 23:17:01',
                    updateTime: '2020-05-23 12:36:21'
                },
                {
                    id: 3,
                    name: 'cdqmxplc',
                    createTime: '2045-06-15 10:48:04',
                    updateTime: '2046-03-20 16:54:28'
                },
                {
                    id: 4,
                    name: 'rdiwafif',
                    createTime: '2025-06-12 09:41:41',
                    updateTime: '2027-01-04 14:36:46'
                }
            ],
            count: 32
        });

        groupApi.listing(1, 3, null, (count, groups) => {
            expect(count).toBe(32);
            expect(groups).toEqual([
                {
                    id: 2,
                    name: 'testUpdate',
                    createTime: '2029-05-26 23:17:01',
                    updateTime: '2020-05-23 12:36:21'
                },
                {
                    id: 3,
                    name: 'cdqmxplc',
                    createTime: '2045-06-15 10:48:04',
                    updateTime: '2046-03-20 16:54:28'
                },
                {
                    id: 4,
                    name: 'rdiwafif',
                    createTime: '2025-06-12 09:41:41',
                    updateTime: '2027-01-04 14:36:46'
                }
            ]);
            done()
        })
    });

    test('更新项目组', done => {
        axios.put.mockResolvedValue({
            group: {
                id: 2,
                name: 'testUpdate',
                createTime: '2029-05-26 23:17:01',
                updateTime: '2020-05-23 12:36:20'
            }
        });

        groupApi.update(2, {name: 'testUpdate'}, group => {
            expect(group).toEqual({
                id: 2,
                name: 'testUpdate',
                createTime: '2029-05-26 23:17:01',
                updateTime: '2020-05-23 12:36:20'
            });
            done()
        })
    });


});
