package com.yanshiqian.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yanshiqian.feign.client.ClassClient;
import com.yanshiqian.feign.client.TeacherClient;
import com.yanshiqian.student.entity.Homeworksubmit;

import com.yanshiqian.student.entity.vo.HomeworksubmitQuery;
import com.yanshiqian.student.mapper.HomeworksubmitMapper;
import com.yanshiqian.student.service.HomeworksubmitService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yanshiqian
 * @since 2021-09-23
 */
@Service
public class HomeworksubmitServiceImpl extends ServiceImpl<HomeworksubmitMapper, Homeworksubmit> implements HomeworksubmitService {
    @Autowired
    private ClassClient client;
    @Autowired
    private TeacherClient teacherClient;

    @Override
    public Homeworksubmit upload(MultipartFile file, Map<String, Object> map,  String courseId, String times,String year) {
       String filepath ="homework/"+client.getCourseName(courseId)+year+"/";
        // String filepath ="D://hometest/"+client.getCourseName(courseId)+year+"/";
        File targetFile = new File(filepath);
        if (!targetFile.exists()) {
            targetFile.mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(filepath + file.getOriginalFilename())){
            out.write(file.getBytes());

        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
        Homeworksubmit res = new Homeworksubmit();
        res.setName((String)map.get("nickname"));
        res.setStuId((String)map.get("username"));
        res.setStuClass((String) map.get("stuClass"));

        res.setCourseId(courseId);
        res.setTimes(times);
        res.setEnd(teacherClient.getEnd(times,courseId));
        String oldFileName = filepath + file.getOriginalFilename();
        // 旧的文件或目录
        File oldName = new File(oldFileName);
        String suffix = oldFileName.substring(oldFileName.lastIndexOf("."));
        // 新的文件或目录
        String newFileName = filepath + client.getCourseName(courseId)+"第"+times+"次作业"+"-"+(String)map.get("nickname")+"-"+(String)map.get("username")+"-"+(String) map.get("stuClass")+suffix;
        File newName = new File(newFileName);
        if (newName.exists()) {  //  确保新的文件名不存在
            newName.delete();
        }
        oldName.renameTo(newName);

        res.setPath(client.getCourseName(courseId)+year+"/"+client.getCourseName(courseId)+"第"+times+"次作业"+"-"+(String)map.get("nickname")+"-"+(String)map.get("username")+"-"+(String) map.get("stuClass")+suffix);
        System.out.println(res);
        baseMapper.insert(res);
        return res;
    }

    @Override
    public void downLoad(HttpServletResponse response,  String filename,String filename2) throws IOException, InvalidFormatException {
        // String filePath = "D://hometest" ;
        String filePath = "homework";

        File file = new File(filePath + "/" + filename+"/"+filename2);
        if(file.exists()){
            response.setContentType("application/octet-stream");
            response.setHeader("content-type", "application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(filename2,"utf8"));
            byte[] buffer = new byte[1024];
            //输出流
            OutputStream os = null;
            try(FileInputStream fis= new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);) {
                os = response.getOutputStream();
                int i = bis.read(buffer);
                while(i != -1){
                    os.write(buffer);
                    i = bis.read(buffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Map<String, Object> listPage(long current, long limit, Map<String, Object> userInfo) {
        Page<Homeworksubmit> page = new Page<>(current,limit);
        QueryWrapper<Homeworksubmit> wrapper = new QueryWrapper<>();
        wrapper.eq("stu_id",(String)userInfo.get("username"));
        baseMapper.selectPage(page,wrapper);
        long total = page.getTotal();//总记录数
        List<Homeworksubmit> records = page.getRecords();//集合

        Map<String,Object> res = new HashMap<>();
        res.put("total",total);
        res.put("rows",records);
        return res;
    }

    @Override
    public void deleteSubmitHomework(String id) {
        baseMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> listPage(long current, long limit, HomeworksubmitQuery homeworksubmitQuery) {
        Page<Homeworksubmit> page = new Page<>(current,limit);
        QueryWrapper<Homeworksubmit> wrapper = new QueryWrapper<>();
        String name = homeworksubmitQuery.getName();
        String classId = homeworksubmitQuery.getCourseId();
        String stuClass = homeworksubmitQuery.getStuClass();
        String begin = homeworksubmitQuery.getBegin();
        String end = homeworksubmitQuery.getEnd();
        String times = homeworksubmitQuery.getTimes();
        String stuId = homeworksubmitQuery.getStuId();
        if(!StringUtils.isEmpty(classId)){
            wrapper.eq("course_id",classId);
        }
        if(!StringUtils.isEmpty(stuId)){
            wrapper.like("stu_id",stuId);
        }
        if(!StringUtils.isEmpty(name)){
            wrapper.like("name",name);
        }
        if(!StringUtils.isEmpty(times)){
            wrapper.like("times",times);
        }
        if(!StringUtils.isEmpty(stuClass)){
            wrapper.like("stu_class",stuClass);
        }
        if(!StringUtils.isEmpty(begin)){
            wrapper.ge("gmt_create",begin);//大于等于
        }
        if(!StringUtils.isEmpty(end)){
            wrapper.le("gmt_create",end);//小于等于
        }
        baseMapper.selectPage(page,wrapper);
        long total = page.getTotal();//总记录数
        List<Homeworksubmit> records = page.getRecords();//集合

        Map<String,Object> res = new HashMap<>();
        res.put("total",total);
        res.put("rows",records);
        return res;
    }

    @Override
    public void sendFile(List<String> fileNameList) throws IOException {
        String filePath = "/homework";

        /*
         * curl 'http://cheatchecker.cupfell.com/receivefile' \
         * -H 'Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6' \
         * -H 'Connection: keep-alive' \
         * -H 'Content-Type: multipart/form-data;
         * boundary=----WebKitFormBoundaryspWJU67Xc6hng6fw' \
         * -H 'Origin: http://cheatchecker.cupfell.com' \
         * -H 'Referer: http://cheatchecker.cupfell.com/docs' \
         * -H 'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
         * (KHTML, like Gecko) Chrome/102.0.5005.63 Safari/537.36 Edg/102.0.1245.33' \
         * -H 'accept: application/json' \
         * --data-raw $'------WebKitFormBoundaryspWJU67Xc6hng6fw\r\nContent-Disposition:
         * form-data; name="filelist";
         * filename="“反邪教警示教育进校园”-“我要签名”活动.pdf"\r\nContent-Type:
         * application/pdf\r\n\r\n\r\n------WebKitFormBoundaryspWJU67Xc6hng6fw\r\
         * nContent-Disposition: form-data; name="filelist";
         * filename="内聚与耦合.pdf"\r\nContent-Type:
         * application/pdf\r\n\r\n\r\n------WebKitFormBoundaryspWJU67Xc6hng6fw--\r\n' \
         * --compressed \
         * --insecure
         */
        // 按照上述curl命令的格式发送请求
           
        // 创建表单
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setCharset(Charset.forName("UTF-8"));
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.setContentType(ContentType.MULTIPART_FORM_DATA);
        // 文件
        for (String fileName : fileNameList) {
            // 挨个加进 filelist 表项里
            File file = new File(filePath + "/" + fileName);
            if(file.exists()){
                builder.addBinaryBody("filelist", file);
            }
        }
        HttpEntity entity = builder.build();
        // 发送请求
        HttpPost httpPost = new HttpPost("http://cheatchecker.cupfell.com/receivefile");
        httpPost.setEntity(entity);
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            response = client.execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HttpEntity responseEntity = response.getEntity();
        String result = EntityUtils.toString(responseEntity);
        System.out.println(result);
    }
}

