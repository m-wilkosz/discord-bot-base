{
  "title": {
    "title": "Help - Command ${command.name} details"
  },
  "color" : {
    "r": 200,
    "g": 0,
    "b": 255
  },
  "description": "Name: **${command.name}**
Description: <#if command.templated >
<#include "${command.name}_description">
<#else>
${command.description}
</#if>

<#if command.help??>
<#if command.help.templated>
Usage: `<#include "${command.name}_usage">`
Detailed help: <#include "${command.name}_long_help">
<#else>
Usage: `${command.help.usage}`
Detailed help: ${command.help.longHelp}
</#if>
<#if command.aliases?? && command.aliases?size gt 0>
Aliases: `${command.aliases?join("`, `")}`
</#if>
Restrictions:
<#if restricted?? && restricted>
Executable by:<#if allowedRoles??> <#list allowedRoles as allowedRole> ${allowedRole.asMention}<#sep>or<#else>Nobody</#list> </#if>
<#if immuneRoles?? > Immune roles: <#list immuneRoles as immuneRole> ${immuneRole.asMention}<#sep>or<#else>None</#list> </#if>
<#else>
Not restricted
</#if>
</#if>
Parameters:
<#if command.parameters??>
<#list command.parameters as parameter>
${parameter.name}: ${(parameter.description)!""}
Optional: ${parameter.optional?string('yes', 'no')}
<#else>
No parameters
</#list>
<#else>
No parameters
</#if>
  "
}