/**
 * 
 */
package com.labbol.forward.controller;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yelong.commons.beans.BeanUtilsE;

import com.labbol.api.support.request.queryinfo.QueryFilterInfo;
import com.labbol.api.support.request.queryinfo.QuerySortInfo;
import com.labbol.api.support.response.APIResponse;
import com.labbol.api.support.response.AbstractCreateAPIResponse;
import com.labbol.api.support.response.AbstractDeleteAPIResponse;
import com.labbol.api.support.response.AbstractModifyAPIResponse;
import com.labbol.api.support.response.QueryAPIResponse;
import com.labbol.cocoon.controller.BaseCocoonController;
import com.labbol.cocoon.controller.BaseCrudSupportController;
import com.labbol.cocoon.msg.JsonFormData;
import com.labbol.cocoon.msg.JsonMsg;
import com.labbol.core.check.login.LoginValidate;
import com.labbol.core.platform.user.model.User;

/**
 * @author PengFei
 */
@LoginValidate
public abstract class BaseForwardController<M> extends BaseCocoonController {

	private static final String DEFAULT_PRIMARY_KEY_FIELD_NAME = "id";

	/**
	 * 修改model注入的前缀
	 */
	@InitBinder
	public void initBinderModel(WebDataBinder binder) {
		binder.setFieldDefaultPrefix("model.");
		// 加入时间解析
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
	}

	/**
	 * 保存和修改均调用此方法 判断是否是保存和修改的方式通过id是否存在进行判断
	 */
	@ResponseBody
	@RequestMapping(value = "save")
	public Object save(@ModelAttribute M model) throws Exception {
		JsonMsg msg = new JsonMsg(false, "数据保存失败！");
		// 验证model
		if (!validateModel(model, msg)) {
			msg.setMsg(StringUtils.isNotBlank(msg.getMsg()) ? msg.getMsg() : "数据验证未通过，保存失败！");
		} else {
			boolean isNew = isNew(model);
			APIResponse apiResponse;
			if (isNew) {
				try {
					BeanUtilsE.setProperty(model, "creator", getLoginUsername());
					BeanUtilsE.setProperty(model, "updator", getLoginUsername());
					BeanUtilsE.setProperty(model, "state", "0");
				} catch (Exception e) {
				}
				beforeSave(model);
				apiResponse = saveModel(model);
				afterSave(model, apiResponse);
			} else {
				try {
					BeanUtilsE.setProperty(model, "creator", getLoginUsername());
					BeanUtilsE.setProperty(model, "updator", getLoginUsername());
					BeanUtilsE.setProperty(model, "state", "0");
				} catch (Exception e) {
				}
				beforeModify(model);
				apiResponse = modifyModel(model);
				afterModify(model, apiResponse);
			}
			if (apiResponse.isSuccess()) {
				msg.setSuccess(true);
				msg.setMsg("保存成功！");
			} else {
				msg.setSuccess(false);
				msg.setMsg(apiResponse.getErrorMsg());
			}
		}
		return toJson(msg);
	}

	protected void beforeSave(M model) {

	}

	protected void beforeModify(M model) {

	}

	/**
	 * @deprecated {@link #afterSave(Object, AbstractCreateAPIResponse)}
	 * @since 1.0.6
	 */
	@Deprecated
	protected void afterSave(M model, APIResponse apiResponse) {
		afterSave(model, (AbstractCreateAPIResponse) apiResponse);
	}

	protected void afterSave(M model, AbstractCreateAPIResponse apiResponse) {

	}

	/**
	 * @deprecated {@link #afterModify(Object, AbstractModifyAPIResponse)}
	 * @since 1.0.6
	 */
	protected void afterModify(M model, APIResponse apiResponse) {
		afterModify(model, (AbstractModifyAPIResponse) apiResponse);
	}

	protected void afterModify(M model, AbstractModifyAPIResponse apiResponse) {

	}

	/**
	 * 是否是新增model
	 */
	protected boolean isNew(M model) throws NoSuchMethodException {
		return StringUtils.isBlank((String) BeanUtilsE.getProperty(model, DEFAULT_PRIMARY_KEY_FIELD_NAME));
	}

	/**
	 * 是否是修改model
	 */
	protected boolean isModify(M model) throws NoSuchMethodException {
		return !isNew(model);
	}

	/**
	 * 保存model。 重写此方法覆盖默认的保存方法
	 */
	protected abstract APIResponse saveModel(M model) throws Exception;

	/**
	 * 修改model 重写此方法覆盖默认的修改方法
	 */
	protected abstract APIResponse modifyModel(M model) throws Exception;

	/**
	 * save时验证model。 未通过验证则不进行保存且返回异常信息。 异常信息可以设置在msg中
	 * 
	 * @param model
	 * @param msg   异常信息
	 * @return <tt>true</tt> 验证成功
	 */
	protected boolean validateModel(M model, JsonMsg msg) {
		return true;
	}

	@ResponseBody
	@RequestMapping(value = "delete")
	public Object delete() throws Exception {
		JsonMsg msg = new JsonMsg(false, "数据删除失败！");
		String deleteIds = getRequest().getParameter("deleteIds");
		if (StringUtils.isBlank(deleteIds)) {
			msg.setMsg("数据删除失败！未发现数据标识！");
		} else {
			deleteIds = beforeDeleteModel(deleteIds);
			APIResponse apiResponse = deleteModel(deleteIds);
			afterDeleteModel(deleteIds, apiResponse);
			if (apiResponse.isSuccess()) {
				msg.setSuccess(true);
				msg.setMsg("删除成功！");
			} else {
				msg.setSuccess(false);
				msg.setMsg(apiResponse.getErrorMsg());
			}
		}
		return toJson(msg);
	}

	protected String beforeDeleteModel(String deleteIds) {
		return deleteIds;
	}

	/**
	 * 删除数据 覆盖此方法重写删除功能
	 * 
	 * @param deleteIds
	 * @return
	 * @throws Exception
	 */
	protected abstract APIResponse deleteModel(String deleteIds) throws Exception;

	/**
	 * @deprecated {@link #afterDeleteModel(String, AbstractDeleteAPIResponse)}
	 */
	protected void afterDeleteModel(String deleteIds, APIResponse apiResponse) {
		afterDeleteModel(deleteIds, (AbstractDeleteAPIResponse) apiResponse);
	}

	protected void afterDeleteModel(String deleteIds, AbstractDeleteAPIResponse apiResponse) {

	}

	@ResponseBody
	@RequestMapping(value = "query", method = RequestMethod.POST)
	public Object query(@ModelAttribute M model) throws Exception {
		JsonMsg msg = new JsonMsg(true, "服务端异常，数据获取失败！");
		beforeQueryModel(model);
		List<QuerySortInfo> querySortInfos = new ArrayList<QuerySortInfo>();

		// 排序
		Map<String, String> sortFieldMap = getSortFieldMap();
		if (MapUtils.isNotEmpty(sortFieldMap)) {
			for (Entry<String, String> entry : sortFieldMap.entrySet()) {
				querySortInfos.add(createQuerySortInfo(entry.getKey(), entry.getValue()));
			}
		}

		// 条件
		List<QueryFilterInfo> queryFilterInfos = new ArrayList<>();
		for (com.labbol.core.queryinfo.filter.QueryFilterInfo queryFilterInfo : getQueryFilterInfos()) {
			QueryFilterInfo q = new QueryFilterInfo();
			org.apache.commons.beanutils.BeanUtils.copyProperties(q, queryFilterInfo);
			queryFilterInfos.add(q);
		}

		Integer pageNum = Integer.valueOf(getRequest().getParameter("page"));
		Integer pageSize = Integer.valueOf(getRequest().getParameter("limit"));

		QueryAPIResponse<?> queryAPIResponse = queryModel(model, queryFilterInfos, querySortInfos, pageNum, pageSize);
		afterQueryModel(model, queryAPIResponse);
		if (!queryAPIResponse.isSuccess()) {
			msg.setSuccess(false);
			msg.setMsg(queryAPIResponse.getErrorMsg());
		}
		return msg.isSuccess() ? toJson(queryAPIResponse.getQueryResult()) : toJson(msg);
	}

	/**
	 * 创建查询排序 重写此方法可以定制字段（如多表查询时添加字段的别名）
	 * 
	 * @param sortField 排序字段
	 * @param direction 排序方向
	 * @return 排序对象
	 */
	protected QuerySortInfo createQuerySortInfo(String sortField, String direction) {
		return new QuerySortInfo(sortField, direction);
	}

	protected void beforeQueryModel(M model) {

	}

	/**
	 * 重写此方法，覆盖查询功能
	 * 
	 * @param model    条件model
	 * @param pageNum  页码
	 * @param pageSize 页面大小
	 * @return 查询信息
	 * @throws Exception
	 */
	protected abstract QueryAPIResponse<?> queryModel(M model, List<QueryFilterInfo> queryFilterInfos,
			List<QuerySortInfo> querySortInfos, Integer pageNum, Integer pageSize) throws Exception;

	protected void afterQueryModel(M model, QueryAPIResponse<?> queryAPIResponse) {

	}

	@ResponseBody
	@RequestMapping(value = "retrieve", method = RequestMethod.POST)
	public Object retrieve(@ModelAttribute M model) throws Exception {
		JsonMsg msg = new JsonMsg(false, "服务端异常，数据获取失败！");
		JsonFormData<M> jsonFormData = null;
		beforeRetrieveModel(model);
		M queryAfterModel = retrieveModel(model);
		afterRetrieveModel(model, queryAfterModel);
		if (null == queryAfterModel) {
			msg.setMsg("数据标识错误，获取数据失败！");
		} else {
			jsonFormData = new JsonFormData<>(true, queryAfterModel);
			msg.setSuccess(true);
		}
		return msg.isSuccess() ? toJson(jsonFormData) : toJson(msg);
	}

	protected void beforeRetrieveModel(M model) {

	}

	protected abstract M retrieveModel(M model) throws Exception;

	/**
	 * 
	 * @param conditionModel 查询信息的model
	 * @param model          查询到的model
	 */
	protected void afterRetrieveModel(M conditionModel, M model) {

	}

	@SuppressWarnings("unchecked")
	protected Class<M> getModelClass() {
		Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(getClass(),
				BaseCrudSupportController.class);
		if (MapUtils.isNotEmpty(typeArguments)) {
			for (Entry<TypeVariable<?>, Type> entry : typeArguments.entrySet()) {
				if (entry.getKey().getName().contentEquals("M")) {
					return (Class<M>) entry.getValue();
				}
			}
		}
		throw new RuntimeException("未发现泛型model");
	}

	public String getLoginUsername() {
		User currentLoginUser = getCurrentLoginUser();
		return currentLoginUser == null ? null : currentLoginUser.getUsername();
	}

	public String getLoginUserRealname() {
		User currentLoginUser = getCurrentLoginUser();
		return currentLoginUser == null ? null : currentLoginUser.getRealName();
	}

}
