package com.galaxyinternet.dict.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.galaxyinternet.dao.dict.DictDao;
import com.galaxyinternet.framework.core.dao.BaseDao;
import com.galaxyinternet.framework.core.service.impl.BaseServiceImpl;
import com.galaxyinternet.model.dict.BatchDictInsetParam;
import com.galaxyinternet.model.dict.Dict;
import com.galaxyinternet.service.DictService;
import com.galaxyinternet.utils.MessageStatus;

import static com.galaxyinternet.utils.ValidationUtil.isNull;
import static com.galaxyinternet.utils.ValidationUtil.isEmptyOrMoreThan;
import static com.galaxyinternet.utils.ValidationUtil.throwPlatformException;
import static com.galaxyinternet.utils.ValidationUtil.isMoreThan;

@Service("com.galaxyinternet.service.DictService")
public class DictServiceImpl extends BaseServiceImpl<Dict>implements DictService {
	//private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private DictDao dictDao;

	@Override
	protected BaseDao<Dict, Long> getBaseDao() {
		return this.dictDao;
	}
	
	@Override
	public int updateById(Dict entity) {
		
		//验证
		isNull(Dict.COMMENT,entity);
		isMoreThan(Dict.NAME, entity.getName(), 32);
		isNull(Dict.ID,entity.getId());
		//
		Dict dict = dictDao.selectById(entity.getId());
		if(dict == null){
			throwPlatformException(MessageStatus.DATA_NOT_EXISTS, "该数据字典不存在");
		}
		entity.setParentCode(entity.getParentCode());
		int count = dictDao.selectCountByParentCodeAndName(entity);
		if(count >0 ){
			throwPlatformException(MessageStatus.SAME_DATA_EXISTS, "该名称已存在");
		}
		entity.setUpdatedTime(System.currentTimeMillis());
		return dictDao.updateById(entity);
	}
	
	@Override
	public int updateByCode(Dict entity) {
		
		//验证
		isNull(Dict.COMMENT,entity);
		isMoreThan(Dict.NAME, entity.getName(), 32);
		isMoreThan(Dict.CODE, entity.getCode(), 32);
		//判断待更新的字典是否存在
		Dict dict = dictDao.selectByCode(entity.getCode());
		if(dict == null){
			throwPlatformException(MessageStatus.DATA_NOT_EXISTS, "该数据字典不存在");
		}
		entity.setId(dict.getId());
		entity.setParentCode(entity.getParentCode());
		
		int count = dictDao.selectCountByParentCodeAndName(entity);
		//判断更新的名字是否重复
		if(count >0 ){
			throwPlatformException(MessageStatus.SAME_DATA_EXISTS, "该名称已存在");
		}
		entity.setUpdatedTime(System.currentTimeMillis());
		return dictDao.updateById(entity);
	}
	
	
	private void validInsert(Dict dict){
		//验证
		isNull(Dict.COMMENT,dict);
		isEmptyOrMoreThan(Dict.NAME, dict.getName(), 32);
		//

	}
	@Override
	public Long insert(Dict entity) {
		
		validInsert(entity);
		isNull(Dict.PARENT_CODE,entity.getParentCode());
		
		Dict parentDict = dictDao.selectByCode(entity.getParentCode());
		if( parentDict == null){
			throwPlatformException(MessageStatus.DATA_NOT_EXISTS,"待添加的数据字典父类型不存在");
		}
		int count = dictDao.selectCountByParentCodeAndName(entity);
		if(count > 0){
			throwPlatformException(MessageStatus.SAME_DATA_EXISTS,"数据已存在");
		}
		Integer max = dictDao.selectMaxValueByParentCode(entity.getParentCode());
		if(max == null){
			max = 1;
		}else {
			max++;
		}
		entity.setValue(max);
		entity.setSort(max);
		entity.setCode(parentDict.getCode()+":"+max);
		entity.setCreatedTime(System.currentTimeMillis());
		return dictDao.insert(entity);
		
	}
	
	
	
	
	@Override
	public List<Dict> selectByParentCode(String parentCode) {
		return dictDao.selectByParentCode(parentCode);
	}


	@Override
	public int insertInBatch(BatchDictInsetParam batchDictInsetParam) {
		
		isNull(Dict.COMMENT,batchDictInsetParam);
		isNull(Dict.PARENT_CODE,batchDictInsetParam.getParentCode());
		
		List<String> names = null;
		List<Dict> dicts = batchDictInsetParam.getDicts();
		
		if(dicts == null ||dicts.isEmpty()){
			throwPlatformException(MessageStatus.FIELD_NOT_ALLOWED_EMPTY, Dict.COMMENT);
		}
		names = new ArrayList<>();
		
		//判断待添加的数据字典父类型是否存在
		Dict parentDict = dictDao.selectByCode(batchDictInsetParam.getParentCode());
		if(parentDict == null){
			throwPlatformException(MessageStatus.DATA_NOT_EXISTS,"待添加的数据字典父类型不存在");
		}
		//得到一个parentCode下最大的value 
		Integer max = dictDao.selectMaxValueByParentCode(batchDictInsetParam.getParentCode());
		if(max == null){
			max = 1;
		}else {
			max++;
		}
		for (Dict dict : dicts) {
			dict.setSort(max);
			dict.setValue(max);
			dict.setCode(parentDict.getCode()+":"+max);
			max ++;
			validInsert(dict);
			//判断待添加的数据字典名字是否有相同的
			if(dict.getName()==null || names.contains(dict.getName())){
				throwPlatformException(MessageStatus.PARAME_SAME, Dict.NAME);
			}else {
				names.add(dict.getName());
			}
		}
		//判断待添加的数据字典名字和数据库已经存在是否有相同的
		int count = dictDao.selectCountSameIn(batchDictInsetParam);
		if(count > 0){
			throwPlatformException(MessageStatus.SAME_DATA_EXISTS,"待添加数据已存在");
		}
		batchDictInsetParam.setCreatedTime(System.currentTimeMillis());
		return dictDao.insertInBatch(batchDictInsetParam);
	}


	@Override
	public Dict selectByCode(String code) {
		isEmptyOrMoreThan(Dict.CODE, code, 32);
		return dictDao.selectByCode(code);
	}


	@Override
	public int updateSort(Dict entity) {

		isNull(Dict.COMMENT,entity);
		isNull(Dict.ID,entity.getId());
		
		entity.setName(null);
		entity.setUpdatedTime(System.currentTimeMillis());
		return dictDao.updateById(entity);
	}

}
