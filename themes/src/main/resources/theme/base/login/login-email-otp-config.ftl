<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('emailCode'); section>
    <#if section="header">
        ${msg("doLogIn")}
    <#elseif section="form">
        <form id="kc-email-login-config-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="col-xs-12" style="display: flex; padding: 0; align-items: flex-end;">
                        <div style="flex: 3; padding-right: 5px;">
                            <label for="emailAddress" class="${properties.kcLabelClass!}">${msg("email")}</label>
                            <input id="emailAddress" name="emailAddress" autocomplete="off" type="text" class="${properties.kcInputClass!}"
                                   autofocus aria-invalid="<#if messagesPerField.existsError('emailAddress')>true</#if>"
                                   value="<#if auth.getEmail()?has_content>${auth.getEmail()}</#if>"/>

                            <#if messagesPerField.existsError('emailAddress')>
                                <span id="input-error-email" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.get('emailAddress'))?no_esc}
                                </span>
                            </#if>
                        </div>

                        <div style="flex: 1;">
                            <input style="height: 36px; width: 100%;" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                                   name="sendConfig" type="submit" value="${msg('sendCode')}"/>
                        </div>
                    </div>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <div class="col-xs-12" style="padding: 0;">
                        <label for="emailCode" class="${properties.kcLabelClass!}">${msg("authenticatorCode")}</label>
                        <#--                        <input id="emailCode" name="emailCode" autocomplete="off" type="text" class="${properties.kcInputClass!}"-->
                        <#--                               autofocus aria-invalid="<#if messagesPerField.existsError('emailCode')>true</#if>"/>-->
                        <div id="otp-container">
                            <input v-for="(digit, index) in codeDigits" :key="index" v-model="codeDigits[index]"
                                   class="otp-input" type="text" maxlength="1"
                                   @input="moveToNext(index)" @keydown.backspace="moveToPrev(index)"
                                   @paste="handlePaste($event)">
                        </div>
                        <input type="hidden" id="emailCode" name="emailCode" v-model="codeValue"/>

                        <#if messagesPerField.existsError('emailCode')>
                            <span id="input-error-otp-code" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('emailCode'))?no_esc}
                            </span>
                        </#if>
                    </div>
                </div>

                <div class="${properties.kcInputWrapperClass!}" style="margin-top: 20px;">
                    <div class="col-xs-12" style="padding: 0;">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="loginConfig" type="submit" value="${msg('doLogIn')}" />
                    </div>
                </div>

            </div>
        </form>
    </#if>
</@layout.registrationLayout>

<script type="text/javascript">
    document.addEventListener('DOMContentLoaded', function () {
        const form = document.getElementById('kc-email-login-config-form');
        form.addEventListener('keypress', function(event) {
            if (event.key === 'Enter') {
                const loginButton = form.querySelector('[name="loginConfig"]');
                if (loginButton) {
                    event.preventDefault();
                    loginButton.click();
                }
            }
        });
    });
</script>

<style>
    #otp-container {
        display: flex;
        gap: 8px;
    }
    .otp-input {
        width: 16.6%;
        height: 40px;
        text-align: center;
        font-size: 20px;
        border: 2px solid #ccc;
        border-radius: 5px;
        outline: none;
        transition: border-color 0.2s;
    }
    .otp-input:focus {
        border-color: #007bff;
    }
</style>

<script type="text/javascript" src="${url.resourcesPath}/js/vue.min.js"></script>
<script>
    new Vue({
        el: '#kc-email-login-config-form',
        data() {
            return {
                codeDigits: ["", "", "", "", "", ""], // 6个输入框
            };
        },
        computed: {
            codeValue() {
                return this.codeDigits.join(""); // 组合成完整验证码
            }
        },
        methods: {
            moveToNext(index) {
                if (this.codeDigits[index] && index < this.codeDigits.length - 1) {
                    this.$nextTick(() => {
                        this.$el.querySelectorAll('.otp-input')[index + 1].focus();
                    });
                }
            },
            moveToPrev(index) {
                if (!this.codeDigits[index] && index > 0) {
                    this.$nextTick(() => {
                        this.$el.querySelectorAll('.otp-input')[index - 1].focus();
                    });
                }
            },
            handlePaste(event) {
                event.preventDefault();
                const pastedData = event.clipboardData.getData('text').slice(0, 6).split('');
                for (let i = 0; i < pastedData.length; i++) {
                    this.codeDigits[i] = pastedData[i];
                }
            }
        }
    });
</script>
