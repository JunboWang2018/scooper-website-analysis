package cn.showclear.www.service.init.impl;

import cn.com.scooper.common.exception.BusinessException;
import cn.showclear.utils.FileConnectUtil;
import cn.showclear.www.common.constant.CommonConstant;
import cn.showclear.www.dao.base.count.CountDao;
import cn.showclear.www.dao.base.file.FileDao;
import cn.showclear.www.dao.base.table.TableDao;
import cn.showclear.www.pojo.base.TableDo;
import cn.showclear.www.service.init.InitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * @author Wang Junbo
 * @description
 * @date 2019/6/18
 */
@Service
public class InitServiceImpl implements InitService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitServiceImpl.class);

    @Autowired
    private TableDao tableDao;

    @Autowired
    private FileDao fileDao;

    @Autowired
    private PropertiesFactoryBean propFactory;

    @Autowired
    private CountDao countDao;


    /**
     * 初始化工具，包含数据库备份和文件时间备份
     * @return
     */
    @Override
    public void init() throws BusinessException {
        //创建备份文件
        boolean initFileResult = FileConnectUtil.initFile();
        if (!initFileResult) {
            LOGGER.error("创建备份文件失败！");
        }
  //      Message initDBInfoResult = this.initDBBackupData();
        this.initFileBackupData();
        this.initCountBackupData();
       /* if (CommonConstant.FAILED_CODE == initDBInfoResult.getCode() && CommonConstant.FAILED_CODE == initFileInfoResult.getCode()) {
            LOGGER.error("初始化备份信息失败！请重试");
        } else if (CommonConstant.FAILED_CODE == initDBInfoResult.getCode()) {
            LOGGER.error(initDBInfoResult.getMessage());
        } else if (CommonConstant.FAILED_CODE == initFileInfoResult.getCode()) {
            LOGGER.error(initFileInfoResult.getMessage());
        } else {
            LOGGER.info("初始化备份信息成功！");
        }*/
       LOGGER.info("初始化完成！");
    }


    private void initDBBackupData() throws BusinessException {
        List<TableDo> tableDoList = null;
        Properties properties = null;
        try {
            properties = propFactory.getObject();
        } catch (IOException e) {
            String excepMsg = "读取数据库配置信息失败！";
            LOGGER.error(excepMsg);
            throw new BusinessException(CommonConstant.FAILED_CODE, excepMsg);
        }
        String backupDBName = properties.getProperty("mainDB.dbname");
        try {
            tableDoList = tableDao.getDBTableInfo(backupDBName);
        } catch (SQLException e) {
            String excepMsg = "初始化备份过程中查询表结构信息失败！";
            LOGGER.error(excepMsg);
            throw new BusinessException(CommonConstant.FAILED_CODE, excepMsg);
        }
        if (tableDoList == null) {
            String excepMsg = "初始化备份过程中查询表结构信息为空！";
            LOGGER.error(excepMsg);
            throw new BusinessException(CommonConstant.FAILED_CODE, excepMsg);
        }
        boolean updatePropDBResult = tableDao.updateDBProperties(tableDoList);
        if (updatePropDBResult) {
            LOGGER.info("初始化备份表结构信息成功！");
        } else {
            LOGGER.error("初始化备份表结构信息失败！");
        }
    }

    private void initFileBackupData() {
        Long fileTime = 0L;
        try {
            fileTime = fileDao.getFileTimeProperty();
        } catch (IOException e) {
            LOGGER.error("获取备份文件时间信息失败！", e);
        }
        //没有设置时间信息，设置!
        if (fileTime == 0) {
            Long time = System.currentTimeMillis();
            boolean updatePropFileResult = fileDao.updateFileTimeProperty(time);
            if (updatePropFileResult) {
                LOGGER.info("初始化备份文件时间信息成功!");
            } else {
                LOGGER.error("初始化备份文件时间信息失败!");
            }
        }
    }
    private void initCountBackupData() {
        if (countDao.getCount() != 0) {
            return;
        }
        countDao.updateCount(0);
    }
}
