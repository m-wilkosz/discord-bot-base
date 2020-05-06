{
  "author": {
    "name": "${threadUser.member.effectiveName}",
    "avatar":  "${threadUser.member.user.effectiveAvatarUrl}"
  },
  "color" : {
    "r": 200,
    "g": 0,
    "b": 255
  },
  <#assign user>${threadUser.member.effectiveName}#${threadUser.member.user.discriminator} (${threadUser.member.user.id})</#assign>
  <#assign joinDate>${threadUser.member.timeJoined}</#assign>
  <#assign roles><#list threadUser.member.roles as role>${role.asMention}<#sep>,<#else>No roles</#list></#assign>
  <#assign threadCount>3</#assign>
  "description": "<#include "modmail_thread_header_embed_description">"
}