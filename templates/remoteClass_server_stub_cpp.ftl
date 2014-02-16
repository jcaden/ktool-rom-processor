cpp/${remoteClass.name}.cpp
/* Autogenerated with Kurento Idl */

#include "${remoteClass.name}.hpp"

namespace kurento {

<#if (remoteClass.constructors[0])??>
std::shared_ptr<MediaObjectImpl> ${remoteClass.name}::Factory::createObject (const Json::Value &params) throw (JsonRpc::CallException)
{
  Json::Value aux;
  <#list remoteClass.constructors[0].params as param>
  ${getCppObjectType(param.type.name, false)} ${param.name};
  </#list>

  <#list remoteClass.constructors[0].params as param>
  if (!params.isMember ("${param.name}") ) {
    <#if (param.defaultValue)??>
    /* param '${param.name}' not present, using default */
    <#if param.type.name = "String" || param.type.name = "int" || param.type.name = "boolean">
    ${param.name} = ${param.defaultValue};
    <#else>
    // TODO, deserialize default param value for type '${param.type}'
    </#if>
    <#else>
    <#if (param.optional)>
    // Warning, optional constructor parameter '${param.name}' but not default value provided
    </#if>
    /* param '${param.name}' not present, raise exception */
    JsonRpc::CallException e (JsonRpc::ErrorCode::SERVER_ERROR_INIT,
                              "'${param.name}' parameter is requiered");
    throw e;
    </#if>
  } else {
    aux = params["${param.name}"];
    <#if param.type.name = "String">
	  <#assign json_method = "String">
	  <#assign type_description = "string">
    <#elseif param.type.name = "int">
	  <#assign json_method = "Int">
	  <#assign type_description = "integer">
    <#elseif param.type.name = "boolean">
	  <#assign json_method = "Bool">
	  <#assign type_description = "boolean">
    </#if>
    <#if (json_method)?? && (type_description)??>

    if (!aux.is${json_method} ()) {
      /* param '${param.name}' has invalid type value, raise exception */
      JsonRpc::CallException e (JsonRpc::ErrorCode::SERVER_ERROR_INIT,
                              "'${param.name}' parameter should be a ${type_description}");
      throw e;
    }

    ${param.name} = aux.as${json_method} ();
    <#else>
    // TODO, deserialize param type '${param.type}'
    </#if>
  }

  </#list>

  return createObject (<#rt>
     <#lt><#list remoteClass.constructors[0].params as param><#rt>
        <#lt>${param.name}<#rt>
        <#lt><#if param_has_next>, </#if><#rt>
     <#lt></#list>);
}
</#if>

} /* kurento */